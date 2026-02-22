package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.Mapper.AirbnbMapper;
import com.example.AirbnbDemo.dtos.CreateAirbnbDTO;
import com.example.AirbnbDemo.exceptions.ResourceNotFoundException;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.repository.writes.AirbnbRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AirbnbService implements IAirbnbService{

    private final AirbnbRepository airbnbRepository;

    @Override
    public Airbnb createAirbnb(CreateAirbnbDTO dto) {
        Airbnb airbnb= AirbnbMapper.toEntity(dto);
        return airbnbRepository.save(airbnb);
    }

    @Override
    public Airbnb getAirbnbById(Long id) {
        return airbnbRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Airbnb with ID :"+id+" not found"));
    }

    @Override
    public List<Airbnb> getAllAirbnbs() {
        return  airbnbRepository.findAll();
    }

    @Override
    public Airbnb updateAirbnb(Long id, CreateAirbnbDTO dto) {
        Airbnb existing = airbnbRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airbnb with ID :"+id+" not found"));
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setPricePerNight(dto.getPricePerNight());
        existing.setLocation(dto.getLocation());
        return airbnbRepository.save(existing);
    }

    @Override
    public void deleteAirbnb(Long id) {
        if(!airbnbRepository.existsById(id)){
            throw new ResourceNotFoundException("Airbnb with id " + id + " not found");
        }
        airbnbRepository.deleteById(id);
    }
}
