package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.Mapper.AirbnbMapper;
import com.example.AirbnbDemo.dtos.CreateAirbnbDTO;
import com.example.AirbnbDemo.exceptions.ResourceNotFoundException;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.readModels.AirbnbReadModel;
import com.example.AirbnbDemo.repository.reads.RedisReadRepository;
import com.example.AirbnbDemo.repository.reads.RedisWriteRepository;
import com.example.AirbnbDemo.repository.writes.AirbnbRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AirbnbService implements IAirbnbService{

    private final AirbnbRepository airbnbRepository;
    private final RedisReadRepository redisReadRepository;
    private final RedisWriteRepository redisWriteRepository;

    @Override
    @Transactional
    public Airbnb createAirbnb(CreateAirbnbDTO dto) {
        Airbnb airbnb= AirbnbMapper.toEntity(dto);
        airbnb=airbnbRepository.save(airbnb);
        redisWriteRepository.writeAirbnb(airbnb);
        return airbnb;
    }

    @Override
    public AirbnbReadModel getAirbnbById(Long id) {
        return redisReadRepository.getAirbnbById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Airbnb with ID :"+id+" not found"));
    }

    @Override
    public List<AirbnbReadModel> getAllAirbnbs() {
        return redisReadRepository.getAllAirbnbs();
    }

    @Override
    @Transactional
    public Airbnb updateAirbnb(Long id, CreateAirbnbDTO dto) {
        Airbnb existing = airbnbRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airbnb with ID :"+id+" not found"));
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setPricePerNight(dto.getPricePerNight());
        existing.setLocation(dto.getLocation());
        Airbnb newAirbnb=airbnbRepository.save(existing);
        redisWriteRepository.writeAirbnb(newAirbnb);
        return newAirbnb;
    }

    @Override
    @Transactional
    public void deleteAirbnb(Long id) {
        if(!airbnbRepository.existsById(id)){
            throw new ResourceNotFoundException("Airbnb with id " + id + " not found");
        }
        airbnbRepository.deleteById(id);
        redisWriteRepository.deleteAirbnb(id);
    }
}
