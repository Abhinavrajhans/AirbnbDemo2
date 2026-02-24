package com.example.AirbnbDemo.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaEventConsumer {

    private static final String SAGA_QUEUE = "saga:events";
    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SagaEventProcessor sagaEventProcessor;

    @Scheduled(fixedDelay = 500) //poll every 500 mili seconds
    public void consumeEvents(){
        try{
            String event=redisTemplate.opsForList().leftPop(SAGA_QUEUE,1, TimeUnit.SECONDS);
            if(event!=null && !event.isEmpty()){
                SagaEvent sagaEvent = objectMapper.readValue(event, SagaEvent.class);
                log.info("Processing SagaEvent {}", sagaEvent.toString());
                sagaEventProcessor.processEvent(sagaEvent);
                log.info("SagaEvent Processed Successfully for saga Id:{}", sagaEvent.toString());
            }
        }
        catch (Exception e){
            log.error("Error Processing saga events: {}", e.getMessage());
            throw new RuntimeException("Failed to process Saga Event",e);
        }
    }
}
