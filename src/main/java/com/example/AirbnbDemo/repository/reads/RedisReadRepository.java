package com.example.AirbnbDemo.repository.reads;

import com.example.AirbnbDemo.models.readModels.AirbnbReadModel;
import com.example.AirbnbDemo.models.readModels.AvailabilityReadModel;
import com.example.AirbnbDemo.models.readModels.BookingReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RedisReadRepository {

    public static final String AIRBNB_KEY_PREFIX = "airbnb:";
    public static final String BOOKING_KEY_PREFIX = "booking:";
    public static final String AVAILABLE_KEY_PREFIX = "available:";

    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;

    public AirbnbReadModel getAirbnbById(Long id){
        String key=AIRBNB_KEY_PREFIX + id;
        String value=redisTemplate.opsForValue().get(key);
        if(value==null)return null;
        try{
            return objectMapper.readValue(value, AirbnbReadModel.class);
        } catch(JacksonException e){
            throw new RuntimeException("Failed to parse AirbnbReadModel from Redis",e);
        }
    }

    public BookingReadModel getBookingById(Long id){
        String key=BOOKING_KEY_PREFIX + id;
        String value=redisTemplate.opsForValue().get(key);
        if(value==null)return null;
        try{
            return objectMapper.readValue(value, BookingReadModel.class);
        } catch(JacksonException e){
            throw new RuntimeException("Failed to parse BookingReadModel from Redis",e);
        }
    }

    public AvailabilityReadModel getAvailabilityById(Long id){
        String key=AVAILABLE_KEY_PREFIX + id;
        String value=redisTemplate.opsForValue().get(key);
        if(value==null)return null;
        try{
            return objectMapper.readValue(value, AvailabilityReadModel.class);
        }catch(JacksonException e){
            throw new RuntimeException("Failed to parse AvailabilityReadModel from Redis",e);
        }
    }

    public List<AirbnbReadModel> getAllAirbnbs(){
        Set<String> keys=redisTemplate.keys(AIRBNB_KEY_PREFIX+"*");
        if (keys == null || keys.isEmpty()) return List.of();
        return keys.stream()
                .map(k -> {
                    String value = redisTemplate.opsForValue().get(k);
                    if (value == null) return null;
                    try {
                        return objectMapper.readValue(value, AirbnbReadModel.class);
                    } catch (JacksonException e) {
                        throw new RuntimeException("Failed to parse AirbnbReadModel from Redis", e);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    public BookingReadModel findBookingByIdempotencyKey(String idempotencyKey){
        Set<String> keys=redisTemplate.keys(BOOKING_KEY_PREFIX+"*");
        if (keys == null || keys.isEmpty()) return null;
        return keys.stream()
                .map(key -> {
                    String value = redisTemplate.opsForValue().get(key);
                    if (value == null) return null;
                    try {
                        BookingReadModel model = objectMapper.readValue(value, BookingReadModel.class);
                        return idempotencyKey.equals(model.getIdempotencyKey()) ? model : null;
                    } catch (JacksonException e) {
                        throw new RuntimeException("Failed to parse BookingReadModel from Redis", e);
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
