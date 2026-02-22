package com.example.AirbnbDemo.Mapper;


import com.example.AirbnbDemo.dtos.AvailabilityDTO;
import com.example.AirbnbDemo.dtos.CreateAvailabilityDTO;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.Availability;

public class AvailabilityMapper {

    public static Availability toEntity(CreateAvailabilityDTO dto, Airbnb airbnb) {
        return Availability.builder()
                .airbnb(airbnb)
                .booking(null)
                .isAvailable(dto.getIsAvailable())
                .date(dto.getDate())
                .build();
    }

    public static AvailabilityDTO toDTO(Availability availability) {
        return AvailabilityDTO.builder()
                .id(availability.getId())
                .airbnb(availability.getAirbnb())
                .booking(availability.getBooking())
                .isAvailable(availability.getIsAvailable())
                .date(availability.getDate())
                .build();
    }
}
