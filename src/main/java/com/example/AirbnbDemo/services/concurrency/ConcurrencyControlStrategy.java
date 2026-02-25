package com.example.AirbnbDemo.services.concurrency;

import com.example.AirbnbDemo.models.Availability;

import java.time.LocalDate;
import java.util.List;

public interface ConcurrencyControlStrategy {

    void releaseLock(Long airbnbId, LocalDate checkIn,LocalDate checkOut,Long userId);
    List<Availability> lockAndCheckAvailability(Long airbnbId, LocalDate checkInDate, LocalDate checkOutDate,Long userId);
}
