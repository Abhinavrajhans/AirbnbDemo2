package com.example.AirbnbDemo.models.readModels;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AirbnbReadModel {
    private Long id;
    private String name;
    private String description;
    private String location;
    private Long pricePerNight;
    private List<AvailabilityReadModel> availability;
}