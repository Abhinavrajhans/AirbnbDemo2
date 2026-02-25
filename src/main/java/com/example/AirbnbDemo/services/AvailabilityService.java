package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.mapper.AvailabilityMapper;
import com.example.AirbnbDemo.dtos.CreateAvailabilityDTO;
import com.example.AirbnbDemo.exceptions.ResourceNotFoundException;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.Availability;
import com.example.AirbnbDemo.models.readModels.AvailabilityReadModel;
import com.example.AirbnbDemo.repository.reads.RedisReadRepository;
import com.example.AirbnbDemo.repository.writes.AirbnbRepository;
import com.example.AirbnbDemo.repository.writes.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService implements IAvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final AirbnbRepository airbnbRepository;
    private final RedisReadRepository redisReadRepository;


    @Override
    @Transactional
    public Availability createAvailability(CreateAvailabilityDTO dto) {
        Airbnb airbnb = airbnbRepository.findById(dto.getAirbnbId())
                .orElseThrow(() -> new ResourceNotFoundException("Airbnb with Id:"+ dto.getAirbnbId() +" not found"));
        Availability availability = AvailabilityMapper.toEntity(dto, airbnb);
        availability=availabilityRepository.save(availability);
        return availability;
    }


    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityReadModel> checkAvailability(Long airbnbId) {
       return redisReadRepository.getAvailabilityByAirbnbId(airbnbId);
    }
}
