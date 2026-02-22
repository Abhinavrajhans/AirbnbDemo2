package com.example.AirbnbDemo.dtos;

import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.Booking;
import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityDTO {
    private Long id;
    private Airbnb airbnb;
    private LocalDate date;
    private Booking booking; // this can be null
    private Boolean isAvailable;
}
