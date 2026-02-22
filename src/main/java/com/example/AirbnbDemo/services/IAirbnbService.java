package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.dtos.CreateAirbnbDTO;
import com.example.AirbnbDemo.models.Airbnb;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface IAirbnbService {
    Airbnb createAirbnb(CreateAirbnbDTO dto);
    Airbnb getAirbnbById(Long id);
    List<Airbnb> getAllAirbnbs();
    Airbnb updateAirbnb(Long id, @Valid @RequestBody CreateAirbnbDTO dto);
    void deleteAirbnb(Long id);

}
