package com.example.AirbnbDemo.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAvailabilityDTO {

    @NotNull(message="airbnbID is Required")
    private Long airbnbId;

    @NotNull(message = "Date is Required")
    private LocalDate date;

    private Long bookingId; // this can be null

    @NotNull(message = "isAvailable is Required")
    private Boolean isAvailable;
}
