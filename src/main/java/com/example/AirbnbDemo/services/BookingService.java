package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.Mapper.BookingMapper;
import com.example.AirbnbDemo.dtos.CreateBookingDTO;
import com.example.AirbnbDemo.dtos.UpdateBookingRequest;
import com.example.AirbnbDemo.exceptions.ResourceNotFoundException;
import com.example.AirbnbDemo.models.*;
import com.example.AirbnbDemo.models.readModels.BookingReadModel;
import com.example.AirbnbDemo.repository.reads.RedisReadRepository;
import com.example.AirbnbDemo.repository.writes.AirbnbRepository;
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
    private final RedisReadRepository redisReadRepository;
    private final ConcurrencyControlStrategy concurrencyControlStrategy;
    private final SagaEventPublisher sagaEventPublisher;

    @Override
    @Transactional
    public Booking createBooking(CreateBookingDTO dto) {
        try {
            log.info("createBooking dto {}", dto);
            User user=userRepository.findById(dto.getUserId())
                    .orElseThrow(()->new ResourceNotFoundException("User with Id :"+ dto.getUserId() +" not found"));
            log.info("Got User for Creating booking {}", user);
            Airbnb airbnb=airbnbRepository.findById(dto.getAirbnbId())
                    .orElseThrow(()->new ResourceNotFoundException("Airbnb with Id :"+ dto.getAirbnbId() +" not found"));
            log.info("Got Airbnb Booking for Creating booking {}", airbnb);
            LocalDate checkIn = dto.getCheckInDate();
            LocalDate checkOut = dto.getCheckOutDate();
            LocalDate realCheckOut = checkOut.minusDays(1);
            if(checkIn.isAfter(checkOut)) {
                throw new RuntimeException("Check-in date must be before Check-out date");
            }
            if(checkOut.isBefore(LocalDate.now())) {
                throw new RuntimeException("Check-out date must be today or in the future");
            }
            if(checkIn.equals(checkOut)) {
                throw new RuntimeException("Check-in and check-out dates cannot be the same");
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

    //        sagaEventPublisher.publishEvent("BOOKING_CREATED","CREATE_BOOKING",payload);

            booking = bookingRepository.save(booking);
            return booking;
        } catch (Exception e) {
            LocalDate checkOut = dto.getCheckOutDate();
            LocalDate realCheckOut = checkOut.minusDays(1);
            concurrencyControlStrategy.releaseLock(dto.getAirbnbId(), dto.getCheckInDate(), realCheckOut ,dto.getUserId());
            throw e;
        }
    }

    @Override
    @Transactional
    public String updateBooking(UpdateBookingRequest request) {
        log.info("Updating Booking for idempotency key {}",request.getIdempotencyKey());
        BookingReadModel bookingReadModel = idempotencyService.findBookingByIdempotencyKey(request.getIdempotencyKey())
                    .orElseThrow(()-> new ResourceNotFoundException("Booking with Idempotency Key:"+request.getIdempotencyKey()+" not found"));
        log.info("booking found for idempotency key {}",request.getIdempotencyKey());
        log.info("booking status {}:",bookingReadModel.getBookingStatus());
        BookingStatus status= BookingStatus.valueOf(bookingReadModel.getBookingStatus());
        if(status != BookingStatus.PENDING) {
            throw new RuntimeException("Booking status is not PENDING It is Already Processed.");
        }

        Map<String, Object> payload = Map.of(
                "bookingId",    bookingReadModel.getId().toString(),
                "airbnbId",     bookingReadModel.getAirbnbId().toString(),
                "userId",       bookingReadModel.getUserId(),
                "checkInDate",  bookingReadModel.getCheckInDate().toString(),
                "checkOutDate", bookingReadModel.getCheckOutDate().toString()
        );
        if(request.getBookingStatus()==BookingStatus.CONFIRMED){
            sagaEventPublisher.publishEvent("BOOKING_CONFIRM_REQUESTED","CONFIRM_BOOKING",payload);
        }
        else if(request.getBookingStatus()==BookingStatus.CANCELLED) {
            sagaEventPublisher.publishEvent("BOOKING_CANCEL_REQUESTED","CANCEL_BOOKING",payload);
        }
        return "Booking Is In Progess......";
    }

    @Override
    @Transactional(readOnly = true)
    public BookingReadModel getBookingById(Long id) {
        return  redisReadRepository.getBookingById(id)
                .orElseThrow(()->new ResourceNotFoundException("Booking with Id :"+ id +" not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingReadModel> getAllBookings() {
        return redisReadRepository.getAllBookings();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getUserBookingHistory(Long userId){
        return bookingRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getAirbnbBookingHistory(Long airbnbId) {
        return bookingRepository.findByAirbnbId(airbnbId);
    }


}
