package com.example.AirbnbDemo.services.handlers;

import com.example.AirbnbDemo.repository.reads.RedisWriteRepository;
import com.example.AirbnbDemo.repository.writes.BookingRepository;
import com.example.AirbnbDemo.saga.SagaEvent;
import com.example.AirbnbDemo.saga.SagaEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingEventHandler {

    private final BookingRepository bookingRepository;
    private final SagaEventPublisher sagaEventPublisher;
    private final RedisWriteRepository redisWriteRepository;


    @Transactional
    public void handleBookingConfirmRequest(SagaEvent sagaEvent) {


    }

    @Transactional
    public void handleBookingCancelRequest(SagaEvent sagaEvent) {

    }



}
