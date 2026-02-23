package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.dtos.CreateBookingDTO;
import com.example.AirbnbDemo.exceptions.ResourceNotFoundException;
import com.example.AirbnbDemo.models.*;
import com.example.AirbnbDemo.repository.writes.AirbnbRepository;
import com.example.AirbnbDemo.repository.writes.AvailabilityRepository;
import com.example.AirbnbDemo.repository.writes.BookingRepository;
import com.example.AirbnbDemo.repository.writes.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
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
    private AvailabilityRepository availabilityRepository;

    @InjectMocks
    private BookingService bookingService;

    private User mockUser;
    private Airbnb mockAirbnb;
    private CreateBookingDTO validDto;
    private List<Availability> availabilities=new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .build();
        // simulate JPA-assigned ID
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
                    .date(LocalDate.now().plusDays(i))  // matches validDto's window
                    .isAvailable(true)
                    .build());
        }


    }

    // ─────────────────────────────────────────────────────────────
    // createBooking — happy path
    // ─────────────────────────────────────────────────────────────

    @Test
    void createBooking_shouldReturnBookingWithCorrectTotalPrice() {
        //Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(airbnbRepository.findById(1L)).thenReturn(Optional.of(mockAirbnb));
        when(availabilityRepository.findByAirbnbIdAndDateBetween(1L, validDto.getCheckInDate(), validDto.getCheckOutDate().minusDays(1)))
                .thenReturn(availabilities);
        when(availabilityRepository.saveAll(anyList())).thenReturn(availabilities);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        //Act
        Booking result = bookingService.createBooking(validDto);
        System.out.println(result.toString());
        //Assert
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
        when(availabilityRepository.findByAirbnbIdAndDateBetween(1L, validDto.getCheckInDate(), validDto.getCheckOutDate().minusDays(1)))
                .thenReturn(availabilities);
        when(availabilityRepository.saveAll(anyList())).thenReturn(availabilities);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking b1 = bookingService.createBooking(validDto);
        Booking b2 = bookingService.createBooking(validDto);

        assertThat(b1.getIdempotencyKey()).isNotEqualTo(b2.getIdempotencyKey());
    }

    @Test
    void createBooking_whenUserNotFound_shouldThrowResourceNotFoundException() {
        //when(userRepository.findById(99L)).thenReturn(Optional.empty());
        validDto.setUserId(99L);

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        Booking b1 = bookingService.createBooking(validDto);
        verify(bookingRepository, never()).save(any());
    }


    /**
     * Simulates N concurrent booking attempts for the same airbnb + dates.
     *
     * WITHOUT a lock/availability check, ALL N threads succeed — this test
     * documents the current (broken) behaviour and serves as a regression
     * baseline.  Once proper locking is added, the assertion should flip to
     * "exactly 1 succeeds, N-1 throw".
     */
    @Test
    void createBooking_concurrentRequests_allSucceedDueToMissingLock() throws InterruptedException {
        int threadCount = 10;

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(airbnbRepository.findById(1L)).thenReturn(Optional.of(mockAirbnb));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);   // all threads start together
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();   // wait for the gun
                    bookingService.createBooking(validDto);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();  // fire!
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // ⚠️  Current behaviour: all 10 succeed (no guard in service).
        // TODO: once locking is added, change this to: assertThat(successCount.get()).isEqualTo(1)
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(failureCount.get()).isEqualTo(0);

        // We also verify that the repo was called 10 times, proving the race
        verify(bookingRepository, times(threadCount)).save(any(Booking.class));
    }
}