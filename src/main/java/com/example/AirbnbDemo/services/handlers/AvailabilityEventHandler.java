package com.example.AirbnbDemo.services.handlers;

import com.example.AirbnbDemo.repository.reads.RedisWriteRepository;
import com.example.AirbnbDemo.repository.writes.AvailabilityRepository;
import com.example.AirbnbDemo.saga.SagaEvent;
import com.example.AirbnbDemo.saga.SagaEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AvailabilityEventHandler {

    private final AvailabilityRepository availabilityRepository;
    private final SagaEventPublisher sagaEventPublisher;
    private final RedisWriteRepository redisWriteRepository;


    public void handleBookingConfirmed(SagaEvent sagaEvent) {
        try{
            Map<String,Object> payload = sagaEvent.getPayload();
            Long bookingid=Long.parseLong(payload.get("bookingId").toString());
            Long airbnbId=Long.parseLong(payload.get("bookingId").toString());
            LocalDate checkInDate =  LocalDate.parse(payload.get("checkInDate").toString());
            LocalDate checkOutDate =  LocalDate.parse(payload.get("checkOutDate").toString());
            Long bookedSlots=availabilityRepository.countByAirbnbIdAndDateBetweenAndBookingIsNotNull(airbnbId,checkInDate,checkOutDate);
            if(bookedSlots>0){
                sagaEventPublisher.publishEvent("BOOKING_CANCEL_REQUESTED","CANCEL_BOOKING",sagaEvent.getPayload());
                throw new RuntimeException("Airbnb is not available for the given dates. Please try again with different dates");
            }
            availabilityRepository.updateBookingIdByAirbnbIdAndDateBetween(bookingid,airbnbId,checkInDate,checkOutDate);
        }
        catch(Exception e){
            sagaEventPublisher.publishEvent("BOOKING_COMPENSATED","COMPENSATE_BOOKING",sagaEvent.getPayload());
            throw new RuntimeException("Failed to update booking",e);
        }
    }

    public void handleBookingCancelled(SagaEvent sagaEvent) {
        try{
            Map<String,Object> payload = sagaEvent.getPayload();
            Long airbnbId=Long.parseLong(payload.get("bookingId").toString());
            LocalDate checkInDate =  LocalDate.parse(payload.get("checkInDate").toString());
            LocalDate checkOutDate =  LocalDate.parse(payload.get("checkOutDate").toString());
            availabilityRepository.clearBookingByAirbnbIdAndDateBetween(airbnbId,checkInDate,checkOutDate);
        }
        catch(Exception e){
            sagaEventPublisher.publishEvent("BOOKING_COMPENSATED","COMPENSATE_BOOKING",sagaEvent.getPayload());
            throw new RuntimeException("Failed to update booking",e);
        }
    }


}
