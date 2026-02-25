package com.example.AirbnbDemo.dlq;

import com.example.AirbnbDemo.saga.SagaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterEventPublisher {

    public static final String DLQ_QUEUE="saga:events:dlq";
    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(SagaEvent sagaEvent,Exception error,int attempts) {
        DeadLetterEvent dlqEvent = DeadLetterEvent.builder()
                .originalEvent(sagaEvent)
                .attemptCount(attempts)
                .errorMessage(error.getMessage())
                .failedAt(LocalDateTime.now())
                .build();
        try{
            redisTemplate.opsForList().leftPush(
                    DLQ_QUEUE,
                    objectMapper.writeValueAsString(dlqEvent)
            );
            log.warn("Event moved to DLQ after {} attempts: {}", attempts, sagaEvent);
        }
        catch (Exception e){
            log.error("CRITICAL: Failed to write to DLQ — event lost: {}", sagaEvent, e);
        }
    }
}
