package com.example.AirbnbDemo.controllers;

import com.example.AirbnbDemo.Mapper.AvailabilityMapper;
import com.example.AirbnbDemo.dtos.AvailabilityDTO;
import com.example.AirbnbDemo.dtos.CreateAvailabilityDTO;
import com.example.AirbnbDemo.models.readModels.AvailabilityReadModel;
import com.example.AirbnbDemo.services.IAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
@Slf4j
public class AvailabilityController {

    private final IAvailabilityService availabilityService;


    @PostMapping
    public ResponseEntity<AvailabilityDTO> create(@Valid @RequestBody CreateAvailabilityDTO dto) {
        log.info("Request to create availability : {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(AvailabilityMapper.toDTO(availabilityService.createAvailability(dto)));
    }



    @GetMapping("/airbnb")
    public ResponseEntity<List<AvailabilityReadModel>> getAllAirbnbs(@RequestParam Long airbnbId) {
        return ResponseEntity.ok(availabilityService.checkAvailability(airbnbId));
    }
}
