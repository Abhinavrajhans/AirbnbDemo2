package com.example.AirbnbDemo.repository.reads;

import com.example.AirbnbDemo.models.readModels.AirbnbReadModel;
import com.example.AirbnbDemo.models.readModels.AvailabilityReadModel;
import com.example.AirbnbDemo.models.readModels.BookingReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisReadRepository {

    public static final String AIRBNB_KEY_PREFIX = "airbnb:";
    public static final String BOOKING_KEY_PREFIX = "booking:";
    public static final String AVAILABLE_KEY_PREFIX = "available:";
    public static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    public static final String AIRBNB_AVAILABILITY_PREFIX = "airbnb:availability:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<AirbnbReadModel> getAirbnbById(Long id) {
        return Optional.ofNullable(getByKey(AIRBNB_KEY_PREFIX + id, AirbnbReadModel.class));
    }

    public Optional<BookingReadModel> getBookingById(Long id) {
        return Optional.ofNullable(getByKey(BOOKING_KEY_PREFIX + id, BookingReadModel.class));
    }

    // Get ALL availability for an airbnb — single round trip O(1)
    public List<AvailabilityReadModel> getAvailabilityByAirbnbId(Long airbnbId) {
        String hashKey = AIRBNB_AVAILABILITY_PREFIX + airbnbId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(hashKey);
        if (entries == null || entries.isEmpty()) return List.of(); // null = cache miss
        return entries.values().stream()
                .map(v -> {
                    try {
                        return objectMapper.readValue((String) v, AvailabilityReadModel.class);
                    } catch (JacksonException e) {
                        throw new RuntimeException("Failed to parse availability", e);
                    }
                })
                .collect(Collectors.toList());
    }

    // Get a single date's availability
    public AvailabilityReadModel getAvailabilityByAirbnbIdAndDate(Long airbnbId, String date) {
        String hashKey = AIRBNB_AVAILABILITY_PREFIX + airbnbId;
        String value = (String) redisTemplate.opsForHash().get(hashKey, date);
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, AvailabilityReadModel.class);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to parse availability", e);
        }
    }

    public List<AirbnbReadModel> getAllAirbnbs() {
        return getAllByPrefix(AIRBNB_KEY_PREFIX, AirbnbReadModel.class);
    }

    public List<BookingReadModel> getAllBookings() { return  getAllByPrefix(BOOKING_KEY_PREFIX, BookingReadModel.class); }

    public BookingReadModel findBookingByIdempotencyKey(String idempotencyKey) {
        log.info("Finding Booking for idempotency key {}", idempotencyKey);
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        String bookingId = redisTemplate.opsForValue().get(key);
        if (bookingId == null) {
            log.info("No idempotency key found in Redis for {}", idempotencyKey);
            return null;
        }
        String bookingKey = BOOKING_KEY_PREFIX + bookingId;
        return getByKey(bookingKey, BookingReadModel.class);
    }

    // ─── private helpers ───────────────────────────────────────────

    private <T> T getByKey(String key, Class<T> type) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, type);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to parse " + type.getSimpleName() + " from Redis", e);
        }
    }

    private <T> List<T> getAllByPrefix(String prefix, Class<T> type) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys == null || keys.isEmpty()) return List.of();
        return keys.stream()
                .map(k -> getByKey(k, type))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}