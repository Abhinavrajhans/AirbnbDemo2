package com.example.AirbnbDemo.controllers;


import com.example.AirbnbDemo.Mapper.AirbnbMapper;
import com.example.AirbnbDemo.dtos.AirbnbDTO;
import com.example.AirbnbDemo.dtos.CreateAirbnbDTO;
import com.example.AirbnbDemo.models.readModels.AirbnbReadModel;
import com.example.AirbnbDemo.services.AirbnbService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/airbnb")
@RequiredArgsConstructor
public class AirbnbController {

    private final AirbnbService airbnbService;

    @PostMapping
    public ResponseEntity<AirbnbDTO> create(@Valid @RequestBody CreateAirbnbDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AirbnbMapper.toDTO(airbnbService.createAirbnb(dto)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirbnbReadModel> getAirbnbById(@PathVariable Long id) {
        AirbnbReadModel airbnb= airbnbService.getAirbnbById(id);
        return ResponseEntity.ok(airbnb);
    }

    @GetMapping
    public ResponseEntity<List<AirbnbReadModel>> getAllAirbnbs() {
        return ResponseEntity.ok(airbnbService.getAllAirbnbs());
    }



    @PutMapping("/{id}")
    public ResponseEntity<AirbnbDTO> updateAirbnb(@PathVariable Long id,@Valid @RequestBody CreateAirbnbDTO request) {
        return ResponseEntity.ok(AirbnbMapper.toDTO(airbnbService.updateAirbnb(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAirbnb(@PathVariable Long id) {
        airbnbService.deleteAirbnb(id);
        return ResponseEntity.noContent().build();
    }

}
