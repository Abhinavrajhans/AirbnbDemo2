package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.dtos.CreateBookingDTO;
import com.example.AirbnbDemo.dtos.UpdateBookingRequest;
import com.example.AirbnbDemo.models.Booking;
import com.example.AirbnbDemo.models.readModels.BookingReadModel;

import java.util.List;

public interface IBookingService {
    Booking createBooking(CreateBookingDTO dto);
    String updateBooking(UpdateBookingRequest request);
    BookingReadModel getBookingById(Long id);
    List<BookingReadModel> getAllBookings();
    List<Booking> getUserBookingHistory(Long userId);
    List<Booking> getAirbnbBookingHistory(Long airbnbId);
}
