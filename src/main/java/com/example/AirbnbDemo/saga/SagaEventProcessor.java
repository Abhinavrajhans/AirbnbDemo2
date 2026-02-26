package com.example.AirbnbDemo.saga;

import com.example.AirbnbDemo.services.handlers.AvailabilityEventHandler;
import com.example.AirbnbDemo.services.handlers.BookingEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventProcessor {

    private final BookingEventHandler bookingEventHandler;
    private final AvailabilityEventHandler availabilityEventHandler;

    public void processEvent(SagaEvent sagaEvent) {
        log.info("Processing SagaEvent In The Saga Event Processor : {}", sagaEvent.toString());
        switch(sagaEvent.getEventType()){
            case "BOOKING_CREATED":
                log.info("Booking Created for booking id: {}",sagaEvent.getPayload().get("bookingId"));
                break;
            case "BOOKING_CONFIRM_REQUESTED":
                bookingEventHandler.handleBookingConfirmRequest(sagaEvent);
                break;
            case "BOOKING_CONFIRMED":
                availabilityEventHandler.handleBookingConfirmed(sagaEvent);
                break;
            case "BOOKING_CANCEL_REQUESTED":
                bookingEventHandler.handleBookingCancelRequest(sagaEvent);
                break;
            case "BOOKING_CANCELLED":
                availabilityEventHandler.handleBookingCancelled(sagaEvent);
                break;
            case "BOOKING_COMPENSATED":
                log.info("Booking compensated for booking id: {}", sagaEvent.getPayload().get("bookingId"));
                availabilityEventHandler.handleBookingCompensated(sagaEvent);
                break;
            case "TEST_FAILURE":
                throw new RuntimeException("Simulated failure for DLQ testing");
            default:
                break;
        }
    }
}

