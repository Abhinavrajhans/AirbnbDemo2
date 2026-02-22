package com.example.AirbnbDemo.Mapper;

import com.example.AirbnbDemo.dtos.BookingDTO;
import com.example.AirbnbDemo.dtos.CreateBookingDTO;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.Booking;
import com.example.AirbnbDemo.models.User;

public class BookingMapper {

    public static Booking toEntity(CreateBookingDTO dto,User user,Airbnb airbnb
            ,String idempotencyKey){
        return Booking.builder()
                .user(user)
                .airbnb(airbnb)
                .totalPrice(dto.getTotalPrice())
                .idempotencyKey(idempotencyKey)
                .checkInDate(dto.getCheckInDate())
                .checkOutDate(dto.getCheckOutDate())
                .build();

    }

    public static BookingDTO toDTO(Booking entity){
        return BookingDTO.builder()
                .user(entity.getUser())
                .airbnb(entity.getAirbnb())
                .totalPrice(entity.getTotalPrice())
                .status(entity.getStatus())
                .checkInDate(entity.getCheckInDate())
                .checkOutDate(entity.getCheckOutDate())
                .build();
    }
}
