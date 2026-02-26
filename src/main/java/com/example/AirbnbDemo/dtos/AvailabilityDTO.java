package com.example.AirbnbDemo.dtos;

import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityDTO {
    private Long airbnbId;
    private LocalDate date;
    private Long bookingId; // this can be null
    private Boolean isAvailable;
}
