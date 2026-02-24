package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.dtos.CreateBookingDTO;
import com.example.AirbnbDemo.dtos.UpdateBookingRequest;
import com.example.AirbnbDemo.models.Booking;

import java.util.List;

public interface IBookingService {
    Booking createBooking(CreateBookingDTO dto);
    Booking updateBooking(UpdateBookingRequest request);
    Booking getBookingById(Long id);
    List<Booking> getAllBookings();
    List<Booking> getUserBookingHistory(Long userId);
    List<Booking> getAirbnbBookingHistory(Long airbnbId);
}
