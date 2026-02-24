package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.models.Booking;

import java.util.Optional;

public interface IIdempotencyService {
    public boolean isIdempotencyKeyUsed(String IdempotencyKey);
    Optional<Booking> findBookingByIdempotencyKey(String bookingId);
}
