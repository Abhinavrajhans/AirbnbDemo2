package com.example.AirbnbDemo.dtos;

import com.example.AirbnbDemo.models.BookingStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
