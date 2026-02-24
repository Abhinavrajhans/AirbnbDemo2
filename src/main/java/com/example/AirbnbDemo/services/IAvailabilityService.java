package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.dtos.CreateAvailabilityDTO;
import com.example.AirbnbDemo.models.Availability;
import com.example.AirbnbDemo.models.readModels.AvailabilityReadModel;
import java.util.List;

public interface IAvailabilityService {
    Availability createAvailability(CreateAvailabilityDTO dto) ;
    List<AvailabilityReadModel> checkAvailability(Long airbnbId) ;
}
