package com.example.AirbnbDemo.services.handlers;

import com.example.AirbnbDemo.exceptions.SagaAlreadyCompensatedException;
import com.example.AirbnbDemo.repository.writes.AvailabilityRepository;
import com.example.AirbnbDemo.saga.SagaEvent;
import com.example.AirbnbDemo.saga.SagaEventPublisher;
import com.example.AirbnbDemo.services.concurrency.ConcurrencyControlStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AvailabilityEventHandler {

    private final AvailabilityRepository availabilityRepository;
    private final SagaEventPublisher sagaEventPublisher;
    private final ConcurrencyControlStrategy concurrencyControlStrategy;

    @Transactional
    public void handleBookingConfirmed(SagaEvent sagaEvent) {
        log.info("Processing SagaEvent In AvailabilityEventHandler {}", sagaEvent.toString());
        try{
            Map<String,Object> payload = sagaEvent.getPayload();
            Long bookingId=Long.parseLong(payload.get("bookingId").toString());
            Long airbnbId=Long.parseLong(payload.get("airbnbId").toString());
            Long userId=Long.parseLong(payload.get("userId").toString());
            LocalDate checkInDate =  LocalDate.parse(payload.get("checkInDate").toString());
            LocalDate checkOutDate =  LocalDate.parse(payload.get("checkOutDate").toString());
            LocalDate realCheckOut = checkOutDate.minusDays(1);
            Long bookedSlots=availabilityRepository.countByAirbnbIdAndDateBetweenAndBookingIsNotNull(airbnbId,checkInDate,realCheckOut);
            if(bookedSlots>0){
                sagaEventPublisher.publishEvent("BOOKING_CANCEL_REQUESTED","CANCEL_BOOKING",sagaEvent.getPayload());
                throw new SagaAlreadyCompensatedException("Airbnb is not available for the given dates. Please try again with different dates , This Booking Will be Cancelled.");
            }
            log.info("updating the availability in db {}", sagaEvent.toString());
            availabilityRepository.updateBookingIdByAirbnbIdAndDateBetween(bookingId,airbnbId,checkInDate,realCheckOut);
            log.info("done updating the availability in db {}", sagaEvent.toString());
            //  DB now permanently records the booking — release the temporary lock
            concurrencyControlStrategy.releaseBookingLock(airbnbId, checkInDate, realCheckOut,userId);
            log.info("Lock released after confirming booking {}", bookingId);
        }
         catch (SagaAlreadyCompensatedException e) {
            // ✅ Re-throw directly — compensation already published above, skip the catch logic
            throw e;
        }
        catch(Exception e){
            sagaEventPublisher.publishEvent("BOOKING_COMPENSATED","COMPENSATE_BOOKING",sagaEvent.getPayload());
            throw new RuntimeException("Failed to update booking",e);
        }
    }

    @Transactional
    public void handleBookingCancelled(SagaEvent sagaEvent) {
        try{
            Map<String,Object> payload = sagaEvent.getPayload();
            Long bookingId=Long.parseLong(payload.get("bookingId").toString());
            Long airbnbId=Long.parseLong(payload.get("airbnbId").toString());
            Long userId=Long.parseLong(payload.get("userId").toString());
            LocalDate checkInDate =  LocalDate.parse(payload.get("checkInDate").toString());
            LocalDate checkOutDate =  LocalDate.parse(payload.get("checkOutDate").toString());
            LocalDate realCheckOut = checkOutDate.minusDays(1);
            availabilityRepository.clearBookingByAirbnbIdAndDateBetween(airbnbId,checkInDate,realCheckOut);
            //  Booking cancelled — release lock so others can book these dates
            concurrencyControlStrategy.releaseBookingLock(airbnbId, checkInDate, realCheckOut,userId);
            log.info("Lock released after cancelling booking for airbnb {}", airbnbId);
        }
         catch (SagaAlreadyCompensatedException e) {
            throw e; // pass through — already handled
        }
        catch(Exception e){
            sagaEventPublisher.publishEvent("BOOKING_COMPENSATED","COMPENSATE_BOOKING",sagaEvent.getPayload());
            throw new RuntimeException("Failed to update booking",e);
        }
    }


}
