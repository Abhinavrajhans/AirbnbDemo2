package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.Mapper.BookingMapper;
import com.example.AirbnbDemo.dtos.CreateBookingDTO;
import com.example.AirbnbDemo.dtos.UpdateBookingRequest;
import com.example.AirbnbDemo.exceptions.ResourceNotFoundException;
import com.example.AirbnbDemo.models.*;
import com.example.AirbnbDemo.repository.writes.AirbnbRepository;
import com.example.AirbnbDemo.repository.writes.AvailabilityRepository;
import com.example.AirbnbDemo.repository.writes.BookingRepository;
import com.example.AirbnbDemo.repository.writes.UserRepository;
import com.example.AirbnbDemo.saga.SagaEventPublisher;
import com.example.AirbnbDemo.services.concurrency.ConcurrencyControlStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService implements IBookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final AirbnbRepository airbnbRepository;
    private final IIdempotencyService  idempotencyService;
    private final AvailabilityRepository availabilityRepository;
    private final ConcurrencyControlStrategy concurrencyControlStrategy;
    private final SagaEventPublisher sagaEventPublisher;

    @Override
    public Booking createBooking(CreateBookingDTO dto) {
        //Missing
        //1. We are not checking that this airbnb is available between
        // checkInDate and checkOutDate or not
        // 2. After we are booking we have to mark the
        // avaialability of the airbnb on those dates as false.
        User user=userRepository.findById(dto.getUserId())
                .orElseThrow(()->new ResourceNotFoundException("User with Id :"+ dto.getUserId() +" not found"));
        Airbnb airbnb=airbnbRepository.findById(dto.getAirbnbId())
                .orElseThrow(()->new ResourceNotFoundException("Airbnb with Id :"+ dto.getAirbnbId() +" not found"));
        LocalDate checkIn = dto.getCheckInDate();
        LocalDate checkOut = dto.getCheckOutDate();
        LocalDate realCheckOut = checkOut.minusDays(1);
        if(checkIn.isAfter(checkOut)) {
            throw new RuntimeException("Check-in date must be before Check-out date");
        }
        if(checkOut.isBefore(LocalDate.now())) {
            throw new RuntimeException("Check-out date must be today or in the future");
        }
        List<Availability> availabilities =
                concurrencyControlStrategy.lockAndCheckAvailability(
                        dto.getAirbnbId(), checkIn, realCheckOut, dto.getUserId()
                );
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double totalPrice = nights * airbnb.getPricePerNight();
        String idempotencyKey = UUID.randomUUID().toString();
        log.info("Creating booking for Airbnb {} | dates: {} → {} | total: {} | key: {}",
                airbnb.getId(), checkIn, checkOut, totalPrice, idempotencyKey);
        Booking booking = BookingMapper.toEntity(dto, user, airbnb, idempotencyKey, totalPrice);
        Map<String, Object> payload = Map.of(
                "bookingId",    booking.getId().toString(),
                "airbnbId",     booking.getAirbnb().getId().toString(),
                "checkInDate",  booking.getCheckInDate().toString(),
                "checkOutDate", booking.getCheckOutDate().toString()
        );
        sagaEventPublisher.publishEvent("BOOKING_CREATED","CREATE_BOOKING",payload);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking updateBooking(UpdateBookingRequest request) {
        log.info("Updating Booking for idempotency key {}",request.getIdempotencyKey());
        Booking booking = idempotencyService.findBookingByIdempotencyKey(request.getIdempotencyKey())
                    .orElseThrow(()-> new ResourceNotFoundException("Booking with Idempotency Key:"+request.getIdempotencyKey()+" not found"));
        log.info("booking found for idempotency key {}",request.getIdempotencyKey());
        log.info("booking status {}:",booking.getStatus());
        if(booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Booking status is not PENDING It is Already Processed.");
        }

        Map<String, Object> payload = Map.of(
                "bookingId",    booking.getId().toString(),
                "airbnbId",     booking.getAirbnb().getId().toString(),
                "checkInDate",  booking.getCheckInDate().toString(),
                "checkOutDate", booking.getCheckOutDate().toString()
        );
        if(request.getBookingStatus()==BookingStatus.CONFIRMED){
            sagaEventPublisher.publishEvent("BOOKING_CONFIRM_REQUESTED","CONFIRM_BOOKING",payload);
        }
        else if(request.getBookingStatus()==BookingStatus.CANCELLED) {
            sagaEventPublisher.publishEvent("BOOKING_CANCEL_REQUESTED","CANCEL_BOOKING",payload);
        }
        return booking;
    }

    @Override
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Booking with Id :"+ id +" not found"));
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public List<Booking> getUserBookingHistory(Long userId){
        return bookingRepository.findByUserId(userId);
    }

    @Override
    public List<Booking> getAirbnbBookingHistory(Long airbnbId) {
        return bookingRepository.findByAirbnbId(airbnbId);
    }


}
