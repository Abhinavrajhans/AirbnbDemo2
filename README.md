# Airbnb Booking System

A production-grade Airbnb-style property booking backend built with **Spring Boot 4**, demonstrating a suite of distributed systems patterns: **CQRS**, **Saga (Choreography)**, **Change Data Capture (CDC)**, **Distributed Locking**, **Idempotency**, and **Dead Letter Queue (DLQ)**. Every design decision was made to handle real-world concurrency, consistency, and fault-tolerance challenges at scale.

---

## Table of Contents

1. [Technology Stack](#technology-stack)
2. [Architecture Overview](#architecture-overview)
3. [Domain Model](#domain-model)
4. [CQRS — Command Query Responsibility Segregation](#cqrs--command-query-responsibility-segregation)
5. [Change Data Capture (CDC) with Debezium + Kafka](#change-data-capture-cdc-with-debezium--kafka)
6. [Saga Pattern — Choreography-Based Distributed Transactions](#saga-pattern--choreography-based-distributed-transactions)
7. [Distributed Locking with Redis](#distributed-locking-with-redis)
8. [Idempotency](#idempotency)
9. [Dead Letter Queue (DLQ)](#dead-letter-queue-dlq)
10. [Composite Primary Key on Availability](#composite-primary-key-on-availability)
11. [Global Exception Handling](#global-exception-handling)
12. [Database Indexes](#database-indexes)
13. [API Reference](#api-reference)
14. [Configuration Reference](#configuration-reference)
15. [Running the Project](#running-the-project)
16. [Development History](#development-history)

---

## Technology Stack

| Layer | Technology |
|---|---|
| Runtime | Java 24 |
| Framework | Spring Boot 4.0.3 |
| ORM | Spring Data JPA / Hibernate |
| Write Database | MySQL 8 |
| Cache / Lock Store | Redis |
| Message Broker | Apache Kafka |
| CDC Connector | Debezium (MySQL source connector) |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Build Tool | Gradle |
| Boilerplate Reduction | Lombok |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────────┐
│                            REST API (Controllers)                        │
│        User │ Airbnb │ Booking │ Availability │ DeadLetterQueue          │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │
                    ┌───────────▼───────────┐
                    │      Service Layer    │
                    │  (Business Logic,     │
                    │   Saga orchestration) │
                    └──────┬──────────┬─────┘
                           │          │
              ┌────────────▼──┐   ┌───▼────────────────┐
              │  Write Path   │   │    Read Path        │
              │  (MySQL / JPA)│   │  (Redis Cache)      │
              └────────────┬──┘   └───▲────────────────┘
                           │          │ Cache populated by
                    Debezium CDC       │ CDC Consumer
                           │          │
              ┌────────────▼──────────┴──┐
              │        Apache Kafka       │
              │  (CDC topics per table)   │
              └───────────────────────────┘

   Redis also serves as:
   ├── Saga event queue  (saga:events  — List)
   ├── Dead Letter Queue (saga:events:dlq — List)
   └── Distributed Lock  (lock:availability:<id>:<in>:<out>)
```

The system is split into two explicit paths:

- **Command side (Write):** All mutations go through MySQL via JPA. Hibernate enforces entity relationships, constraints, and audit timestamps.
- **Query side (Read):** All reads are served from Redis. The cache is populated asynchronously by CDC consumers that listen to Debezium change events on Kafka topics — **the application never writes to Redis directly on the write path**.

---

## Domain Model

### Entity Relationship

```
User  ────────────< Booking >──────────── Airbnb
                      │                     │
                      │                     └──────< Availability
                      │                                  (airbnb_id, date) ← composite PK
                      └─── references Availability slots via booking_id FK
```

### `User`
Stores guest information. Has a `@Column(unique = true)` constraint on `email`. Indexed on `email` for fast duplicate-check queries.

### `Airbnb`
Represents a property listing. Owns `@OneToMany` relationships to both `Booking` and `Availability` with `CascadeType.ALL` and `orphanRemoval = true`, meaning deleting an Airbnb cascades to all its bookings and availability slots.

### `Booking`
The core transactional entity. Key design decisions:
- **Status lifecycle:** `PENDING → CONFIRMED | CANCELLED` — enforced by the Saga.
- **`idempotencyKey`:** A UUID generated at creation time, stored as a `unique` column. Used to prevent duplicate bookings and to look up existing bookings safely.
- **`checkInDate` / `checkOutDate`:** Both stored. The system internally uses `checkOut - 1 day` (`realCheckOut`) for inclusive date-range queries, since checkout day itself is not an occupied night.
- **Indexes:** `idx_booking_user_id`, `idx_booking_airbnb_id` for performant history lookups.

### `Availability`
Represents one bookable night slot for a property. Key design decisions:
- **Composite Primary Key `(airbnb_id, date)`:** enforced via JPA `@IdClass`. See [Composite Primary Key on Availability](#composite-primary-key-on-availability).
- **`isAvailable` flag:** `true` by default. Set to `false` when a booking is confirmed.
- **`booking_id` FK:** `null` for open slots; set to the confirmed booking ID when occupied.
- **Audit timestamps:** `createdDate` (immutable) and `updatedAt` via Spring Data's `@CreatedDate` / `@LastModifiedDate` and `@EntityListeners(AuditingEntityListener.class)`.

### `BaseModel`
Abstract superclass for `User`, `Airbnb`, `Booking`. Provides:
- Auto-generated `@Id` (`@GeneratedValue(strategy = GenerationType.IDENTITY)`)
- `@CreatedDate` / `@LastModifiedDate` audit fields
- `@EntityListeners(AuditingEntityListener.class)` for automatic population

---

## CQRS — Command Query Responsibility Segregation

CQRS is an architectural pattern that separates the model used for **writing data (commands)** from the model used for **reading data (queries)**. This allows each side to be optimized independently.

### Write Side

All commands (create booking, update status, etc.) write to **MySQL** through JPA repositories:

- `UserRepository`, `AirbnbRepository`, `BookingRepository`, `AvailabilityRepository` — all extend `JpaRepository<T, ID>`.
- Mutations are wrapped in `@Transactional` service methods to guarantee **ACID** (Atomicity, Consistency, Isolation, Durability) semantics.

### Read Side

All queries are served from **Redis** using `RedisReadRepository`. Redis key namespaces:

| Namespace | Redis Type | Content |
|---|---|---|
| `airbnb:<id>` | String (JSON) | `AirbnbReadModel` |
| `booking:<id>` | String (JSON) | `BookingReadModel` |
| `idempotency:<key>` | String | booking ID |
| `availability:airbnb:<id>` | Hash (field = date) | `AvailabilityReadModel` per date |

The availability cache uses a **Redis Hash** keyed by `airbnb_id`, with each field being a date string (`"2026-03-15"`). This enables `O(1)` retrieval of all availability slots for a property in a single `HGETALL` round trip, versus scanning multiple keys.

### Read-Through / Cache-Miss Fallback

Every read service method follows this pattern:

```java
List<AvailabilityReadModel> cached = redisReadRepository.getAvailabilityByAirbnbId(airbnbId);
if (!cached.isEmpty()) return cached;
log.warn("Cache miss for airbnb {}, falling back to DB", airbnbId);
return availabilityRepository.findByAirbnbId(airbnbId)...
```

A cache miss falls back to MySQL and logs a warning, but does **not** re-populate Redis from the service layer. Re-population only happens through CDC — this keeps the write path free of cache-write concerns.

### Read Models

`AirbnbReadModel`, `BookingReadModel`, `AvailabilityReadModel` are flat POJOs (no JPA proxies, no lazy-loaded associations). They are stored as serialized JSON in Redis, eliminating the N+1 query problem entirely on the read path.

---

## Change Data Capture (CDC) with Debezium + Kafka

**Change Data Capture (CDC)** is a pattern where changes in a database (inserts, updates, deletes) are captured from the database's binary transaction log and published as events, rather than polling the database or triggering application-level notifications.

**Debezium** is a CDC platform that tails MySQL's **binary log (binlog)** and emits row-level change events to **Kafka topics**. This is a **log-based CDC** approach — it reads from `binlog` positions, not from database queries, so it has near-zero overhead on the write database.

### Kafka Topics (per table)

| Topic | Populated by |
|---|---|
| `airbnb.airbnbspringdemo.airbnbs` | Debezium MySQL connector |
| `airbnb.airbnbspringdemo.bookings` | Debezium MySQL connector |
| `airbnb.airbnbspringdemo.availabilities` | Debezium MySQL connector |

Topic naming convention: `<connector-name>.<database>.<table>`.

### CDC Consumers

Three `@KafkaListener` components consume these topics:

**`AirbnbCDCConsumer`**
- Deserializes the Debezium envelope's `payload` node.
- On `__deleted=true`: removes the key from Redis (`redisTemplate.delete(...)`).
- On upsert: maps payload to `AirbnbReadModel` and stores as JSON string at `airbnb:<id>`.

**`BookingCDCConsumer`**
- Maps payload to `BookingReadModel`, stores at `booking:<id>`.
- Also writes the reverse-lookup index: `idempotency:<key>` → `<bookingId>`. This is how idempotency lookups remain O(1) even as booking counts grow.

**`AvailabilityCDCConsumer`**
- Debezium serializes `DATE` columns as **epoch days** (integer), not ISO strings. The consumer converts: `LocalDate.ofEpochDay(epochDays)`.
- Stores the model in a **Redis Hash** at `availability:airbnb:<airbnbId>`, with the date string as the hash field.

### Why CDC Instead of Application-Level Cache Writes?

| Approach | Problem |
|---|---|
| Write to cache inside `@Transactional` | Cache write can succeed but DB transaction can roll back — stale data |
| Write to cache after transaction commits | Race condition: another read can happen between DB commit and cache write |
| **CDC via Debezium + Kafka** | Cache is updated only after the DB transaction has durably committed to the binlog — strictly consistent |

CDC guarantees that the Redis cache is an **eventually consistent** replica of the MySQL write store, with updates flowing through a reliable, ordered Kafka topic.

---

## Saga Pattern — Choreography-Based Distributed Transactions

A **Saga** is a sequence of local transactions, where each step publishes an event that triggers the next step. If any step fails, **compensating transactions** are published to undo prior steps. This system uses **choreography-based saga** (no central orchestrator — each service reacts to events).

### Why a Saga for Booking Confirmation?

Confirming a booking requires two distinct writes that must be atomic at the business level:
1. Update `Booking.status` from `PENDING` → `CONFIRMED` (MySQL — `bookings` table)
2. Mark the `Availability` slots as occupied: set `isAvailable=false`, `booking_id=<id>` (MySQL — `availabilities` table)

These happen in separate service classes (`BookingEventHandler`, `AvailabilityEventHandler`). A single database transaction could span both, but the saga approach decouples them, making each step independently retryable and observable.

### Event Flow — Booking Confirmation

```
Client
  │
  ▼
BookingService.updateBooking(CONFIRM)
  │ publishes
  ▼
[saga:events] ← Redis List (event queue)
  │ consumed by SagaEventConsumer (@Scheduled, 500ms poll)
  ▼
SagaEventProcessor → BookingEventHandler.handleBookingConfirmRequest()
  │  sets Booking.status = CONFIRMED
  │  publishes "BOOKING_CONFIRMED"
  ▼
[saga:events]
  │
  ▼
SagaEventProcessor → AvailabilityEventHandler.handleBookingConfirmed()
  │  verifies no double-booking (countByAirbnbIdAndDateBetweenAndBookingIsNotNull)
  │  updates Availability slots (bulk UPDATE query)
  │  releases distributed lock
  ▼
  Done
```

### Event Flow — Booking Cancellation

```
BookingService.updateBooking(CANCEL)
  │ publishes "BOOKING_CANCEL_REQUESTED"
  ▼
BookingEventHandler.handleBookingCancelRequest()
  │  sets Booking.status = CANCELLED
  │  publishes "BOOKING_CANCELLED"
  ▼
AvailabilityEventHandler.handleBookingCancelled()
  │  clears Availability slots (booking_id=NULL, isAvailable=true)
  │  releases distributed lock
  ▼
  Done
```

### Compensation Flow

If `BookingEventHandler` or `AvailabilityEventHandler` throws an exception:
1. They publish `BOOKING_COMPENSATED`.
2. `SagaEventProcessor` handles `BOOKING_COMPENSATED` → calls `AvailabilityEventHandler.handleBookingCompensated()`.
3. `handleBookingCompensated()` releases the distributed lock (best-effort, with TTL as final fallback).

This prevents the distributed lock from being held for the full 5-minute TTL on saga failures.

### `SagaEvent` Object

```java
SagaEvent {
    String sagaId;       // UUID — unique per saga execution
    String eventType;    // e.g. "BOOKING_CONFIRMED"
    String step;         // e.g. "CONFIRM_BOOKING"
    Map<String,Object> payload; // bookingId, airbnbId, userId, checkInDate, checkOutDate
    LocalDateTime timestamp;
    SagaStatus status;   // STARTED, IN_PROGRESS, COMPLETED, FAILED, COMPENSATED
}
```

### Redis as the Event Bus

Saga events are stored as a **Redis List** (`saga:events`). `SagaEventPublisher` does `RPUSH` (right-push); `SagaEventConsumer` does `BLPOP`-style `leftPop` (left-pop with a 1-second block timeout). This gives **FIFO ordering** with no external message broker required for the saga channel.

`SagaEventConsumer` is driven by `@Scheduled(fixedDelay = 500)` — it polls every 500 ms. Errors inside the consumer are **caught and logged** (not re-thrown), because throwing from a `@Scheduled` method causes Spring's task scheduler to suppress future runs in certain configurations.

### `RetryableSagaProcessor`

Wraps `SagaEventProcessor.processEvent()` with a configurable retry loop and exponential back-off:

```
attempt 1 → fail → wait 1s
attempt 2 → fail → wait 2s
attempt 3 → fail → move to DLQ
```

The back-off formula is `retryDelayMs * 2^(attempt-1)` — doubling on each attempt (binary exponential back-off). Both `maxAttempts` and `retryDelayMs` are externalized to `application.properties`.

---

## Distributed Locking with Redis

**Distributed locking** prevents race conditions when multiple concurrent requests attempt to book the same property for overlapping dates.

### Problem: TOCTOU (Time-of-Check / Time-of-Use) Race

Without a lock:
```
Thread A: checks availability → available
Thread B: checks availability → available   ← same result, both see open slots
Thread A: creates booking                   ← both succeed
Thread B: creates booking                   ← double-booking!
```

### Solution: Redis `SET NX EX` (SetIfAbsent)

Redis's `SET key value NX EX ttl` is an atomic operation. Only one caller wins the race:

```
redisTemplate.opsForValue().setIfAbsent(lockKey, userId, Duration.ofMinutes(5))
```

- **Key:** `lock:availability:<airbnbId>:<checkIn>:<checkOut>` — scoped to the exact property + date range.
- **Value:** The `userId` — used as the lock owner token.
- **TTL:** 5 minutes (configurable via `booking.lock.ttl-minutes`) — prevents lock leaks if the holder crashes before releasing.

### Lock Lifecycle

```
BookingService.createBooking()
├── lockAndCheckAvailability()  ← acquires lock
│   ├── setIfAbsent → fail     → throw IllegalStateException (409 CONFLICT to client)
│   └── setIfAbsent → success
│       ├── countBookedSlots → > 0 → NOT released here (BookingService catches and releases)
│       └── returns available Availability list
│
├── [booking logic — save Booking to DB]
│
├── on exception → finally block → releaseBookingLock()  ← explicit release
│
└── on success → lock held until saga confirms
    │
    AvailabilityEventHandler.handleBookingConfirmed()
    └── releaseBookingLock()   ← released after DB is permanently updated
         OR
    AvailabilityEventHandler.handleBookingCompensated()
    └── releaseBookingLock()   ← released on saga failure/compensation
```

The lock is **intentionally held** between `createBooking()` returning and the saga completing, blocking any other booking attempt for those dates during the in-flight saga. This prevents the window where a second request could see the availability as open before the first saga has finished updating it.

### Lua Script for Safe Lock Release

Releasing a lock naively (`DEL key`) can accidentally delete a lock owned by a different request if:
- The original holder's TTL expired.
- A second request acquired the same lock.
- The original holder then tries to release it.

The system uses a **Lua script** executed atomically by Redis:

```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

The `GET` + conditional `DEL` is atomic within the Lua execution context — Redis guarantees no other command runs between them. The lock is only deleted if the caller's value (userId / UUID) matches the stored value, ensuring **only the owner can release its own lock**.

### Update Booking Lock

A separate, shorter-lived lock prevents concurrent updates to the same booking:

- **Key:** `lock:booking:update:<bookingId>`
- **Value:** A random UUID (generated per request)
- **TTL:** 10 seconds (configurable via `booking.lock.update-ttl-seconds`)
- **Release:** In a `finally` block in `updateBooking()` — always released after the saga event is published, regardless of success or failure.

---

## Idempotency

**Idempotency** guarantees that submitting the same request multiple times has the same effect as submitting it once. This protects against duplicate bookings caused by network retries or double-clicks.

### How It Works

1. On `createBooking()`, a `UUID.randomUUID()` is generated as the `idempotencyKey` and saved on the `Booking` entity (unique column in MySQL).
2. On `updateBooking()`, the client provides this key in `UpdateBookingRequest.idempotencyKey`.
3. `IdempotencyService.findBookingByIdempotencyKey()` checks:
   - Redis first: `idempotency:<key>` → `<bookingId>` → `booking:<bookingId>` (O(1) two-hop lookup)
   - MySQL fallback: `bookingRepository.findByIdempotencyKey(key)`
4. If the booking is not in `PENDING` status, `updateBooking()` throws `IllegalStateException` (409), preventing re-processing of an already confirmed or cancelled booking.

The CDC consumer for bookings (`BookingCDCConsumer`) is responsible for writing the `idempotency:<key>` → `<bookingId>` index to Redis, keeping the write path clean.

---

## Dead Letter Queue (DLQ)

A **Dead Letter Queue** is a holding area for messages that could not be processed successfully after all retry attempts. It prevents message loss and allows failed events to be inspected and replayed manually.

### Flow

```
RetryableSagaProcessor (3 attempts, exponential back-off)
  └── all attempts exhausted
      └── DeadLetterEventPublisher.publish()
          └── LPUSH saga:events:dlq [DeadLetterEvent JSON]
```

### `DeadLetterEvent` Schema

```json
{
  "originalEvent": { ...SagaEvent... },
  "errorMessage":  "...",
  "attemptCount":  3,
  "failedAt":      "2026-02-26T14:32:00"
}
```

### DLQ Monitor

`DeadLetterQueueMonitor` runs a `@Scheduled(fixedDelay = 60_000)` job that checks the DLQ size every minute and emits a `WARN` log if there are unprocessed events. This provides passive alerting without requiring an external monitoring system.

### DLQ Replay API

`DeadLetterQueueController` exposes endpoints to inspect and replay DLQ events manually. `DeadLetterQueueService` pops events from `saga:events:dlq`, deserializes them, and re-pushes the `originalEvent` back into `saga:events` for reprocessing.

---

## Composite Primary Key on Availability

The `Availability` entity uses a **composite primary key** `(airbnb_id, date)` implemented via JPA's `@IdClass` pattern.

### `@IdClass` Pattern

```java
@Entity
@IdClass(AvailabilityId.class)
public class Availability {
    @Id @ManyToOne
    @JoinColumn(name = "airbnb_id")
    private Airbnb airbnb;

    @Id
    private LocalDate date;
}

public class AvailabilityId implements Serializable {
    private Long airbnb;   // matches Airbnb's PK type
    private LocalDate date;
}
```

The field name in `AvailabilityId` (`airbnb`) matches the `@Id` field name in `Availability` (`airbnb`). For `@ManyToOne @Id` fields, the type in the `@IdClass` must be the **PK type of the referenced entity** (`Long`), not the entity itself.

### Why a Composite PK Instead of a Surrogate Key?

A surrogate auto-increment ID would allow inserting two rows with the same `(airbnb_id, date)` — the uniqueness guarantee would depend entirely on a separate `UNIQUE` constraint. With a composite PK, the database engine **structurally prevents** duplicate availability slots. There is no row for `(airbnb=1, date=2026-03-15)` and another row for `(airbnb=1, date=2026-03-15)`.

### JPA `save()` Behavior with Composite PKs

JPA's `save()` calls `EntityManager.merge()`. For entities with an existing PK (composite or otherwise), `merge()` issues an `UPDATE` — not an `INSERT`. It does not raise a constraint violation. To detect duplicates at the application level, `AvailabilityService.createAvailability()` explicitly calls `availabilityRepository.existsById(id)` before saving and throws `AvailabilityAlreadyExistsException` (409 CONFLICT) if the slot already exists.

---

## Global Exception Handling

`GlobalExceptionHandler` is annotated with `@RestControllerAdvice`, which applies it as an AOP (Aspect-Oriented Programming) advice to all `@Controller` classes globally. It maps exceptions to structured JSON responses:

| Exception | HTTP Status | Reason |
|---|---|---|
| `AvailabilityAlreadyExistsException` | 409 CONFLICT | Duplicate availability slot creation |
| `UserEmailAlreadyExistsException` | 409 CONFLICT | Duplicate user email |
| `ResourceNotFoundException` | 404 NOT FOUND | Entity not found in DB |
| `MethodArgumentNotValidException` | 400 BAD REQUEST | Bean Validation (`@NotNull`, etc.) failures |
| `IllegalStateException` | 409 CONFLICT | Lock acquisition failure, invalid booking status transition |
| `RuntimeException` | 400 BAD REQUEST | General unchecked exceptions |
| `Exception` | 500 INTERNAL SERVER ERROR | Catch-all for unexpected errors |

**Handler ordering matters:** `RuntimeException` is declared before `Exception` because `RuntimeException extends Exception`. Spring `@RestControllerAdvice` respects method declaration order within the same class for handler resolution.

All error responses follow a consistent envelope:
```json
{
  "timestamp": "2026-02-26T14:32:00",
  "message":   "...",
  "status":    409
}
```

---

## Database Indexes

Beyond primary keys, the following indexes are defined to optimize frequent query patterns:

| Table | Index | Column(s) | Justification |
|---|---|---|---|
| `users` | `idx_user_email` | `email` | Unique constraint + lookup by email for deduplication |
| `bookings` | `idx_booking_user_id` | `user_id` | `getUserBookingHistory(userId)` — frequent query |
| `bookings` | `idx_booking_airbnb_id` | `airbnb_id` | `getAirbnbBookingHistory(airbnbId)` — frequent query |
| `availabilities` | PK `(airbnb_id, date)` | composite | All availability queries are scoped to `airbnb_id` + date range |

The composite PK on `availabilities` doubles as the primary clustered index in MySQL's InnoDB engine, meaning rows are physically stored in `(airbnb_id, date)` order. This makes date-range queries (`BETWEEN startDate AND endDate` for a given `airbnb_id`) highly efficient — they are contiguous on-disk scans with no additional index needed.

---

## API Reference

### User

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/user/create` | Create a new user |
| `GET` | `/user/{id}` | Get user by ID |
| `GET` | `/user/all` | Get all users |

### Airbnb

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/airbnb/create` | Create a new property listing |
| `GET` | `/airbnb/{id}` | Get property by ID |
| `GET` | `/airbnb/all` | Get all properties |

### Availability

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/availability/create` | Create an availability slot for a property + date |
| `GET` | `/availability/check/{airbnbId}` | Get all availability slots for a property |

### Booking

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/booking/create` | Create a new booking (status: PENDING) |
| `PUT` | `/booking/update` | Confirm or cancel a booking (triggers Saga) |
| `GET` | `/booking/{id}` | Get booking by ID |
| `GET` | `/booking/all` | Get all bookings |
| `GET` | `/booking/user/{userId}` | Get booking history for a user |
| `GET` | `/booking/airbnb/{airbnbId}` | Get booking history for a property |

### Dead Letter Queue

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/dlq/size` | Get the current number of events in the DLQ |
| `POST` | `/dlq/replay` | Replay a single DLQ event |
| `POST` | `/dlq/replayAll` | Replay all DLQ events |

### Swagger UI

Available at: `http://localhost:3000/swagger-ui.html`

---

## Configuration Reference

All configurable values are externalized in `src/main/resources/application.properties`:

```properties
# ── Database ──────────────────────────────────────────
spring.datasource.url=jdbc:mysql://localhost:3306/airbnbspringdemo
spring.datasource.username=root
spring.datasource.password=root@123
spring.jpa.hibernate.ddl-auto=update

# ── Redis ─────────────────────────────────────────────
spring.data.redis.host=localhost
spring.data.redis.port=6379

# ── Kafka (CDC consumers) ─────────────────────────────
spring.kafka.bootstrap-servers=localhost:9094
spring.kafka.consumer.group-id=airbnb-cdc-group
spring.kafka.consumer.auto-offset-reset=earliest

# ── Server ────────────────────────────────────────────
server.port=3000

# ── Distributed Lock TTLs ─────────────────────────────
booking.lock.ttl-minutes=5          # TTL for booking creation lock
booking.lock.update-ttl-seconds=10  # TTL for booking update lock

# ── Saga Retry ────────────────────────────────────────
saga.retry.max-attempts=3           # Attempts before DLQ
saga.retry.delay-ms=1000            # Base delay for exponential back-off
```

All lock and retry values have sensible defaults (same as the listed values) so the application starts correctly even if the properties are omitted.

---

## Running the Project

### Prerequisites

- Java 24
- MySQL 8 running on port 3306, database `airbnbspringdemo` created
- Redis running on port 6379
- Apache Kafka running on port 9094
- Debezium MySQL connector configured and running (pointing to the same MySQL instance)

### Start

```bash
./gradlew bootRun
```

The application starts on port 3000. Swagger UI is available at `http://localhost:3000/swagger-ui.html`.

### Debezium Connector Configuration (example)

```json
{
  "name": "airbnb-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "localhost",
    "database.port": "3306",
    "database.user": "root",
    "database.password": "root@123",
    "database.server.id": "1",
    "topic.prefix": "airbnb",
    "database.include.list": "airbnbspringdemo",
    "schema.history.internal.kafka.bootstrap.servers": "localhost:9094",
    "schema.history.internal.kafka.topic": "schema-changes.airbnbspringdemo"
  }
}
```

---

## Development History

This project was built incrementally, with each commit addressing a specific feature or correctness concern. Key milestones:

| Commit | Change |
|---|---|
| `1b4e780` | Initial models and User CRUD API |
| `4bfc32e` | Availability and Booking CRUD APIs |
| `39233f0` | `RedisLockStrategy` — distributed locking foundation |
| `68a758a` | `RedisReadRepository` — Redis read path |
| `9a0240c` | `IdempotencyService` — duplicate booking prevention |
| `58c4923` | `SagaEventConsumer`, `SagaEventPublisher`, `SagaEventProcessor` |
| `93bb6e0` | `BookingEventHandler` and `AvailabilityEventHandler` |
| `e1596d7` | CDC consumers using Debezium + Kafka |
| `654c1b9` | `RetryableSagaProcessor` + DLQ logic |
| `349eb24` | Lua script for atomic lock release (fixed wrong-owner release bug) |
| `42eca6c` | Fixed: DLQ size called twice, lock released before acquired, duplicate `realCheckOut` computation |
| `d89b0c9` | Lock lifecycle refactor: `BookingService` fully owns lock acquire + release, `lockAndCheckAvailability` only acquires + checks |
| `de0a060` | Added lock for `updateBooking` to prevent concurrent status transitions |
| `6c42650` | Proper error handling in `updateBooking` with typed exception re-throw |
| `4191957` | Composite PK `(airbnb_id, date)` on `Availability` |
| `316729a` | `AvailabilityId` `@IdClass` — correct type mapping for `@ManyToOne @Id` |
| `fd75dcc` | `AvailabilityAlreadyExistsException` — explicit duplicate detection because JPA `save()` silently UPDATEs on existing composite PK |
| `1c5a333` | `handleBookingCompensated()` — explicit lock release on saga compensation to avoid holding lock for full TTL on failure |
