package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.dtos.CreateAvailabilityDTO;
import com.example.AirbnbDemo.models.Availability;
import java.util.List;

public interface IAvailabilityService {
    Availability createAvailability(CreateAvailabilityDTO dto) ;
    List<Availability> checkAvailability(Long airbnbId) ;
}
