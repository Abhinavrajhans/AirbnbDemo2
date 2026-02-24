package com.example.AirbnbDemo.services.handlers;

import com.example.AirbnbDemo.repository.writes.AvailabilityRepository;
import com.example.AirbnbDemo.saga.SagaEvent;
import com.example.AirbnbDemo.saga.SagaEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvailabilityEventHandler {

    private final AvailabilityRepository availabilityRepository;
    private final SagaEventPublisher sagaEventPublisher;


    public void handleBookingConfirmed(SagaEvent sagaEvent) {

    }

    public void handleBookingCancelled(SagaEvent sagaEvent) {

    }


}
