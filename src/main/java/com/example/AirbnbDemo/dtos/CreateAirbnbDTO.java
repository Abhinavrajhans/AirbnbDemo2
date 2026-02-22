package com.example.AirbnbDemo.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateAirbnbDTO {

    @NotBlank(message="Name is Required")
    private String name;

    @NotBlank(message = "Description is Required")
    private String description;

    @NotNull(message = "pricePerNight is Required")
    private Long pricePerNight;

    @NotBlank(message = "Location is Required")
    private String location;
}
