package com.example.AirbnbDemo.services.handlers;

import com.example.AirbnbDemo.exceptions.ResourceNotFoundException;
import com.example.AirbnbDemo.models.Booking;
import com.example.AirbnbDemo.models.BookingStatus;
import com.example.AirbnbDemo.repository.reads.RedisWriteRepository;
import com.example.AirbnbDemo.repository.writes.BookingRepository;
import com.example.AirbnbDemo.saga.SagaEvent;
import com.example.AirbnbDemo.saga.SagaEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingEventHandler {

    private final BookingRepository bookingRepository;
    private final SagaEventPublisher sagaEventPublisher;
    private final RedisWriteRepository redisWriteRepository;


    @Transactional
    public void handleBookingConfirmRequest(SagaEvent sagaEvent) {
        try{
            Map<String,Object> payload = sagaEvent.getPayload();
            Long bookingid=Long.parseLong(payload.get("bookingId").toString());
            Long airbnbId=Long.parseLong(payload.get("airbnbId").toString());
            LocalDate checkInDate =  LocalDate.parse(payload.get("checkInDate").toString());
            LocalDate checkOutDate =  LocalDate.parse(payload.get("checkOutDate").toString());
            Booking booking=bookingRepository.findById(bookingid)
                    .orElseThrow(()-> new ResourceNotFoundException("Booking With Id:"+bookingid+" Not Found"));
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            redisWriteRepository.writeBooking(booking);
            sagaEventPublisher.publishEvent("BOOKING_CONFIRMED","CONFIRM_BOOKING",sagaEvent.getPayload());
        }
        catch(Exception e){
            sagaEventPublisher.publishEvent("BOOKING_COMPENSATED","COMPENSATE_BOOKING",sagaEvent.getPayload());
            throw new RuntimeException("Failed to comfirm booking",e);
        }
    }

    @Transactional
    public void handleBookingCancelRequest(SagaEvent sagaEvent) {
        try{
            Map<String,Object> payload = sagaEvent.getPayload();
            Long bookingid=Long.parseLong(payload.get("bookingId").toString());
            Long airbnbId=Long.parseLong(payload.get("airbnbId").toString());
            LocalDate checkInDate =  LocalDate.parse(payload.get("checkInDate").toString());
            LocalDate checkOutDate =  LocalDate.parse(payload.get("checkOutDate").toString());
            Booking booking=bookingRepository.findById(bookingid)
                    .orElseThrow(()-> new ResourceNotFoundException("Booking With Id:"+bookingid+" Not Found"));
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            redisWriteRepository.writeBooking(booking);
            sagaEventPublisher.publishEvent("BOOKING_CANCELLED","CANCEL_BOOKING",sagaEvent.getPayload());
        }
        catch(Exception e){
            sagaEventPublisher.publishEvent("BOOKING_COMPENSATED","COMPENSATE_BOOKING",sagaEvent.getPayload());
            throw new RuntimeException("Failed to cancel booking",e);
        }

    }



}
