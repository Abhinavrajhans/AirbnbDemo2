package com.example.AirbnbDemo.Mapper;

import com.example.AirbnbDemo.dtos.BookingDTO;
import com.example.AirbnbDemo.dtos.CreateBookingDTO;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.Booking;
import com.example.AirbnbDemo.models.BookingStatus;
import com.example.AirbnbDemo.models.User;
import com.example.AirbnbDemo.models.readModels.BookingReadModel;
import tools.jackson.databind.JsonNode;

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
                .idempotencyKey(entity.getIdempotencyKey())
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


    public static Booking ToEntityFromReadModel(BookingReadModel bookingReadModel,User user,Airbnb airbnb){
        Booking booking = Booking.builder()
                .user(user)
                .airbnb(airbnb)
                .totalPrice(bookingReadModel.getTotalPrice())
                .status(BookingStatus.valueOf(bookingReadModel.getBookingStatus()))
                .idempotencyKey(bookingReadModel.getIdempotencyKey())
                .checkInDate(bookingReadModel.getCheckInDate())
                .checkOutDate(bookingReadModel.getCheckOutDate())
                .build();
        booking.setId(bookingReadModel.getId());
        return booking;
    }

    public static BookingReadModel ToReadModelFromCDC(Long id, String idempotencyKey,JsonNode payload){

        // ← Both dates are epoch days integers, same as availability
        LocalDate checkInDate = LocalDate.ofEpochDay(payload.path("check_in_date").intValue());
        LocalDate checkOutDate = LocalDate.ofEpochDay(payload.path("check_out_date").intValue());
        return BookingReadModel.builder()
                .id(id)
                .userId(payload.path("user_id").longValue())
                .airbnbId(payload.path("airbnb_id").longValue())
                .totalPrice(payload.path("total_price").doubleValue())
                .bookingStatus(payload.path("status").stringValue())
                .idempotencyKey(idempotencyKey)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .build();
    }
}
