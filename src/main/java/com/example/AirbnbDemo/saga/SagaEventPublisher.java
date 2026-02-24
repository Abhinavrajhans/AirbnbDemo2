package com.example.AirbnbDemo.saga;

import com.example.AirbnbDemo.Mapper.SagaEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SagaEventPublisher {
    // This class is voilating a solid principal
    public static final String SAGA_QUEUE = "saga:events";
    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publishEvent(String eventType, String step, Map<String,Object> payload){
        String sagaId = UUID.randomUUID().toString();
        SagaEvent sagaEvent = SagaEventMapper.toEntity(sagaId,eventType,step,payload);
        try{
            String value = objectMapper.writeValueAsString(sagaEvent);
            redisTemplate.opsForList().rightPush(SAGA_QUEUE,value);
        }
        catch (Exception e){
            throw new RuntimeException("Failed to publish the sagae Event",e);
        }
    }
}
