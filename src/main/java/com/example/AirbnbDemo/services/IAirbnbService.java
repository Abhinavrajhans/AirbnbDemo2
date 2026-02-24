package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.dtos.CreateAirbnbDTO;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.readModels.AirbnbReadModel;

import java.util.List;

public interface IAirbnbService {
    Airbnb createAirbnb(CreateAirbnbDTO dto);
    AirbnbReadModel getAirbnbById(Long id);
    List<AirbnbReadModel> getAllAirbnbs();
    Airbnb updateAirbnb(Long id,CreateAirbnbDTO dto);
    void deleteAirbnb(Long id);

}
