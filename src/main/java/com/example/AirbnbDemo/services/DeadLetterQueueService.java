package com.example.AirbnbDemo.services;

import com.example.AirbnbDemo.dlq.DeadLetterEvent;
import com.example.AirbnbDemo.dlq.DeadLetterEventPublisher;
import com.example.AirbnbDemo.saga.RetryableSagaProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueService implements IDeadLetterQueueService {


    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RetryableSagaProcessor retryableSagaProcessor;


    @Override
    public Long getDlqSize() {
        return redisTemplate.opsForList().size(DeadLetterEventPublisher.DLQ_QUEUE);
    }

    @Override
    public List<DeadLetterEvent> listEvents() {
        Long size=getDlqSize();
        if(size==null || size==0)return List.of();
        return redisTemplate.opsForList()
                .range(DeadLetterEventPublisher.DLQ_QUEUE,0,size-1)
                .stream()
                .map(json->{
                    try{
                        return  objectMapper.readValue(json, DeadLetterEvent.class);
                    }
                    catch(Exception e){
                        log.error("Failed to deserialize DLQ event: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public String replayOne() {
        String topEvent = redisTemplate.opsForList().rightPop(DeadLetterEventPublisher.DLQ_QUEUE);
        if(topEvent==null)return "DLQ is empty";

        try{
            DeadLetterEvent deadLetterEvent = objectMapper.readValue(topEvent, DeadLetterEvent.class);
            log.info("Replaying DLQ event: {}", deadLetterEvent.getOriginalEvent());
            retryableSagaProcessor.processWithRetry(deadLetterEvent.getOriginalEvent());
            return "Replayed successfully: " + deadLetterEvent.getOriginalEvent().getEventType();
        }
        catch(Exception e){
            log.error("Replay failed: {}", e.getMessage());
            return "Replay failed: " + e.getMessage();
        }
    }

    //Replay All events in dlq
    @Override
    public String replayAll() {
        int success=0,failed=0;
        Long size= getDlqSize();
        long snapshot = size == null ? 0 : size;
        for (long i = 0; i < snapshot; i++) {
            try{
                String topEvent = redisTemplate.opsForList().rightPop(DeadLetterEventPublisher.DLQ_QUEUE);
                if (topEvent == null) break;
                DeadLetterEvent deadLetterEvent = objectMapper.readValue(topEvent, DeadLetterEvent.class);
                log.info("Replaying DLQ event: {}", deadLetterEvent.getOriginalEvent());
                retryableSagaProcessor.processWithRetry(deadLetterEvent.getOriginalEvent());
                success++;
            } catch (Exception e) {
                log.error("Replay failed for event: {}", e.getMessage());
                failed++;
            }
        }
        return String.format("Replay complete — success: %d, failed: %d", success, failed);
    }

    @Override
    // Clear the entire DLQ (use carefully)
    public String clearDlq() {
        redisTemplate.delete(DeadLetterEventPublisher.DLQ_QUEUE);
        return "DLQ cleared successfully";
    }
}
