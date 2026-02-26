package com.example.AirbnbDemo.mapper;


import com.example.AirbnbDemo.dtos.AvailabilityDTO;
import com.example.AirbnbDemo.dtos.CreateAvailabilityDTO;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.Availability;
import com.example.AirbnbDemo.models.readModels.AvailabilityReadModel;
import tools.jackson.databind.JsonNode;

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
                .airbnbId(availability.getAirbnb().getId())
                .bookingId(availability.getBooking() != null ? availability.getBooking().getId() : null)
                .isAvailable(availability.getIsAvailable())
                .date(availability.getDate())
                .build();
    }

    public static AvailabilityReadModel toReadModel(Availability availability) {
        return AvailabilityReadModel.builder()
                .airbnbId(availability.getAirbnb().getId())
                .date(availability.getDate().toString())
                .bookingId(availability.getBooking() != null ? availability.getBooking().getId() : null)
                .isAvailable(availability.getIsAvailable())
                .build();
    }

    public static AvailabilityReadModel toReadModelFromCDC(Long airbnbId, String date, JsonNode payload) {
        return AvailabilityReadModel.builder()
                .airbnbId(airbnbId)
                .date(date)
                .bookingId(payload.path("booking_id").isNull() ? null : payload.path("booking_id").longValue())
                .isAvailable(payload.path("is_available").booleanValue())
                .build();
    }
}
