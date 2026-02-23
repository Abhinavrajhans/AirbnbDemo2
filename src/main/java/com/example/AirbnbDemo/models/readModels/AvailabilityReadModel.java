package com.example.AirbnbDemo.models.readModels;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityReadModel {
    private Long id;
    private Long airbnbId;
    private String date;
    private Long bookingId;
    private Boolean isAvailable;
}