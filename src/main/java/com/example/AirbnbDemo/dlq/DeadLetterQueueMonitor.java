package com.example.AirbnbDemo.dlq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueMonitor {

    private final RedisTemplate<String,String> redisTemplate;

    @Scheduled(fixedDelay = 60_000)
    public void reportDlqSize() {
        Long size=redisTemplate.opsForList().size(DeadLetterEventPublisher.DLQ_QUEUE);
        if(size!=null && size>0){
            log.warn("DLQ has {} unprocessed failed events", size);
        }
    }
}
