package com.example.AirbnbDemo.dtos;

import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.BookingStatus;
import com.example.AirbnbDemo.models.User;
import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDTO {
    private Long id;
    private Long userId;
    private Long airbnbId;
    private double totalPrice;
    private BookingStatus status;
    private String idempotencyKey;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}
