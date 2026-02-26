package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.dtos.CreateBookingDTO;
import com.example.AirbnbDemo.exceptions.ResourceNotFoundException;
import com.example.AirbnbDemo.models.*;
import com.example.AirbnbDemo.repository.reads.RedisReadRepository;
import com.example.AirbnbDemo.repository.writes.AirbnbRepository;
import com.example.AirbnbDemo.repository.writes.BookingRepository;
import com.example.AirbnbDemo.repository.writes.UserRepository;
import com.example.AirbnbDemo.saga.SagaEventPublisher;
import com.example.AirbnbDemo.services.concurrency.ConcurrencyControlStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AirbnbRepository airbnbRepository;

    @Mock
    private IIdempotencyService idempotencyService;

    @Mock
    private RedisReadRepository redisReadRepository;

    @Mock
    private ConcurrencyControlStrategy concurrencyControlStrategy;

    @Mock
    private SagaEventPublisher sagaEventPublisher;

    @InjectMocks
    private BookingService bookingService;

    private User mockUser;
    private Airbnb mockAirbnb;
    private CreateBookingDTO validDto;
    private List<Availability> availabilities = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .build();
        mockUser.setId(1L);

        mockAirbnb = Airbnb.builder()
                .name("Beachside Villa")
                .description("Nice place")
                .pricePerNight(200L)
                .location("Goa")
                .build();
        mockAirbnb.setId(1L);

        validDto = CreateBookingDTO.builder()
                .userId(1L)
                .airbnbId(1L)
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(4))
                .build();

        for (int i = 1; i < 10; i++) {
            availabilities.add(Availability.builder()
                    .airbnb(mockAirbnb)
                    .booking(null)
                    .date(LocalDate.now().plusDays(i))
                    .isAvailable(true)
                    .build());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // createBooking — happy path
    // ─────────────────────────────────────────────────────────────

    @Test
    void createBooking_shouldReturnBookingWithCorrectTotalPrice() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(airbnbRepository.findById(1L)).thenReturn(Optional.of(mockAirbnb));
        when(concurrencyControlStrategy.lockAndCheckAvailability(
                eq(1L), any(LocalDate.class), any(LocalDate.class), eq(1L)))
                .thenReturn(availabilities);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createBooking(validDto);

        // 3 nights × 200 = 600
        assertThat(result.getTotalPrice()).isEqualTo(600.0);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.getIdempotencyKey()).isNotBlank();
        assertThat(result.getUser()).isEqualTo(mockUser);
        assertThat(result.getAirbnb()).isEqualTo(mockAirbnb);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_shouldGenerateUniqueIdempotencyKeys() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(airbnbRepository.findById(1L)).thenReturn(Optional.of(mockAirbnb));
        when(concurrencyControlStrategy.lockAndCheckAvailability(
                eq(1L), any(LocalDate.class), any(LocalDate.class), eq(1L)))
                .thenReturn(availabilities);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking b1 = bookingService.createBooking(validDto);
        Booking b2 = bookingService.createBooking(validDto);

        assertThat(b1.getIdempotencyKey()).isNotEqualTo(b2.getIdempotencyKey());
    }

    @Test
    void createBooking_whenUserNotFound_shouldThrowResourceNotFoundException() {
        validDto.setUserId(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(validDto));

        verify(bookingRepository, never()).save(any());
    }

    /**
     * Simulates N concurrent booking attempts for the same airbnb + dates.
     *
     * The lock strategy mock is configured so that only the first caller acquires
     * the lock; all subsequent callers get an IllegalStateException — matching
     * the real RedisLockStrategy behaviour.
     */
    @Test
    void createBooking_concurrentRequests_onlyOneSucceedsDueToLock() throws InterruptedException {
        int threadCount = 10;

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(airbnbRepository.findById(1L)).thenReturn(Optional.of(mockAirbnb));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // First caller acquires the lock and gets availabilities; all others are rejected
        when(concurrencyControlStrategy.lockAndCheckAvailability(
                anyLong(), any(LocalDate.class), any(LocalDate.class), anyLong()))
                .thenReturn(availabilities)
                .thenThrow(new IllegalStateException(
                        "Could not acquire lock for this booking window. Try again."));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    bookingService.createBooking(validDto);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }
}
