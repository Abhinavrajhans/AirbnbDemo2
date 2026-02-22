package com.example.AirbnbDemo.dtos;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AirbnbDTO {
    private Long id;
    private String name;
    private String description;
    private Long pricePerNight;
    private String location;
}
