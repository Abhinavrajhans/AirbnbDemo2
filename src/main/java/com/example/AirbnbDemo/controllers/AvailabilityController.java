package com.example.AirbnbDemo.controllers;

import com.example.AirbnbDemo.Mapper.AvailabilityMapper;
import com.example.AirbnbDemo.dtos.AvailabilityDTO;
import com.example.AirbnbDemo.dtos.CreateAvailabilityDTO;
import com.example.AirbnbDemo.services.IAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final IAvailabilityService availabilityService;


    @PostMapping
    public ResponseEntity<AvailabilityDTO> create(@Valid @RequestBody CreateAvailabilityDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AvailabilityMapper.toDTO(availabilityService.createAvailability(dto)));
    }



    @GetMapping("/airbnb")
    public ResponseEntity<List<AvailabilityDTO>> getAllAirbnbs(@RequestParam Long airbnbId) {
        List<AvailabilityDTO> availabilityDTOList=availabilityService.checkAvailability(airbnbId).stream().map(AvailabilityMapper::toDTO).toList();
        return ResponseEntity.ok(availabilityDTOList);
    }
}
