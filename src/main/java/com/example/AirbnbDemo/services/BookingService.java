package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.Mapper.BookingMapper;
import com.example.AirbnbDemo.dtos.CreateBookingDTO;
import com.example.AirbnbDemo.exceptions.ResourceNotFoundException;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.Availability;
import com.example.AirbnbDemo.models.Booking;
import com.example.AirbnbDemo.models.User;
import com.example.AirbnbDemo.repository.writes.AirbnbRepository;
import com.example.AirbnbDemo.repository.writes.AvailabilityRepository;
import com.example.AirbnbDemo.repository.writes.BookingRepository;
import com.example.AirbnbDemo.repository.writes.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService implements IBookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final AirbnbRepository airbnbRepository;
    private final AvailabilityRepository availabilityRepository;

    @Override
    public Booking createBooking(CreateBookingDTO dto) {
        User user=userRepository.findById(dto.getUserId())
                .orElseThrow(()->new ResourceNotFoundException("User with Id :"+ dto.getUserId() +" not found"));
        Airbnb airbnb=airbnbRepository.findById(dto.getAirbnbId())
                .orElseThrow(()->new ResourceNotFoundException("Airbnb with Id :"+ dto.getAirbnbId() +" not found"));

        LocalDate checkIn = dto.getCheckInDate();
        LocalDate checkOut = dto.getCheckOutDate();
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double totalPrice = nights * airbnb.getPricePerNight();
        //Missing
        //1. We are not checking that this airbnb is available between
        // checkInDate and checkOutDate or not
        // 2. After we are booking we have to mark the
        // avaialability of the airbnb on those dates as false.

        String idempotencyKey = UUID.randomUUID().toString();
        Booking booking = BookingMapper.toEntity(dto,user,airbnb,idempotencyKey,totalPrice);

        Booking savedbooking = bookingRepository.save(booking);
        List<Availability> availabilities = availabilityRepository.findByAirbnbIdAndDateBetween(airbnb.getId(), checkIn, checkOut.minusDays(1));
        for(Availability availability : availabilities) {
            availability.setBooking(booking);
            availability.setIsAvailable(false);
        }
        availabilityRepository.saveAll(availabilities);
        return savedbooking;
    }

    @Override
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Booking with Id :"+ id +" not found"));
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }


    @Override
    public List<Booking> getUserBookingHistory(Long userId){
        return bookingRepository.findByUserId(userId);
    }

    @Override
    public List<Booking> getAirbnbBookingHistory(Long airbnbId) {
        return bookingRepository.findByAirbnbId(airbnbId);
    }


}
