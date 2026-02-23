package com.example.AirbnbDemo.Mapper;

import com.example.AirbnbDemo.dtos.BookingDTO;
import com.example.AirbnbDemo.dtos.CreateBookingDTO;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.Booking;
import com.example.AirbnbDemo.models.User;
import com.example.AirbnbDemo.models.readModels.BookingReadModel;

import java.time.LocalDate;

public class BookingMapper {

    public static Booking toEntity(CreateBookingDTO dto,User user,Airbnb airbnb
            ,String idempotencyKey,double totalPrice){
        return Booking.builder()
                .user(user)
                .airbnb(airbnb)
                .totalPrice(totalPrice)
                .idempotencyKey(idempotencyKey)
                .checkInDate(dto.getCheckInDate())
                .checkOutDate(dto.getCheckOutDate())
                .build();

    }

    public static BookingDTO toDTO(Booking entity){
        return BookingDTO.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .airbnbId(entity.getAirbnb().getId())
                .totalPrice(entity.getTotalPrice())
                .status(entity.getStatus())
                .checkInDate(entity.getCheckInDate())
                .checkOutDate(entity.getCheckOutDate())
                .createdAt(entity.getCreatedDate())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static BookingReadModel toReadModel(Booking booking){
        return BookingReadModel.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .airbnbId(booking.getAirbnb().getId())
                .totalPrice(booking.getTotalPrice())
                .bookingStatus(booking.getStatus().name())
                .idempotencyKey(booking.getIdempotencyKey())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .build();
    }
}
