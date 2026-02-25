package com.example.AirbnbDemo.Mapper;

import com.example.AirbnbDemo.dtos.AirbnbDTO;
import com.example.AirbnbDemo.dtos.CreateAirbnbDTO;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.readModels.AirbnbReadModel;
import tools.jackson.databind.JsonNode;



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

    public static AirbnbReadModel toReadModel(Airbnb airbnb){
        return AirbnbReadModel.builder()
                .id(airbnb.getId())
                .name(airbnb.getName())
                .description(airbnb.getDescription())
                .location(airbnb.getLocation())
                .pricePerNight(airbnb.getPricePerNight())
//                .availability(availabilityReadModel)
                .build();
    }

    public static AirbnbReadModel toReadModelFromCDC(JsonNode payload){
        return AirbnbReadModel.builder()
                .id(payload.path("id").longValue())
                .name(payload.path("name").stringValue())               // ← stringValue()
                .description(payload.path("description").stringValue()) // ← stringValue()
                .location(payload.path("location").stringValue())       // ← stringValue()
                .pricePerNight(payload.path("price_per_night").longValue())
                .build();
    }
}
