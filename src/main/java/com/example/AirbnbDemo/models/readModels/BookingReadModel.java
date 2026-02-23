package com.example.AirbnbDemo.models.readModels;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingReadModel {
    private Long id;
    private Long airbnbId;
    private Long userId;
    private double totalPrice;
    private String bookingStatus;
    private String idempotencyKey;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
}