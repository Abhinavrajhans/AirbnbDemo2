package com.example.AirbnbDemo.Mapper;

import com.example.AirbnbDemo.dtos.AirbnbDTO;
import com.example.AirbnbDemo.dtos.CreateAirbnbDTO;
import com.example.AirbnbDemo.models.Airbnb;

public class AirbnbMapper {

    public static Airbnb toEntity(CreateAirbnbDTO dto){
        return Airbnb.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .pricePerNight(dto.getPricePerNight())
                .location(dto.getLocation())
                .build();
    }

    public static AirbnbDTO toDTO(Airbnb entity){
        return AirbnbDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .pricePerNight(entity.getPricePerNight())
                .location(entity.getLocation())
                .build();
    }
}
