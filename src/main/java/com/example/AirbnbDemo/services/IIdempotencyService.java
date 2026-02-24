package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.models.readModels.BookingReadModel;

import java.util.Optional;

public interface IIdempotencyService {
    public boolean isIdempotencyKeyUsed(String IdempotencyKey);
    Optional<BookingReadModel> findBookingByIdempotencyKey(String bookingId);
}
