package com.example.AirbnbDemo.saga;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor

public class SagaEventConsumer {

    private static final String SAGA_QUEUE = "saga:events";
    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 500) //poll every 500 mili seconds
    public void consumeEvents(){

    }

}
