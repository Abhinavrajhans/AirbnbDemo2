package com.example.AirbnbDemo.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateBookingDTO {

    @NotNull(message="User Id is Required")
    private Long userId;
    @NotNull(message="Airbnb Id is Required")
    private Long airbnbId;
    @NotNull(message="CheckInDate is Required")
    private LocalDate checkInDate;
    @NotNull(message="CheckOutDate is Required")
    private LocalDate checkOutDate;



}
