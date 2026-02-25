package com.example.AirbnbDemo.controllers;

import com.example.AirbnbDemo.saga.SagaEvent;
import com.example.AirbnbDemo.saga.SagaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private static final String SAGA_QUEUE = "saga:events";
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping("/push-failing-event")
    public String pushFailingEvent() throws Exception {
        SagaEvent event = SagaEvent.builder()
                .sagaId(UUID.randomUUID().toString())
                .eventType("TEST_FAILURE")       // ← hits the throwing case
                .step("TEST_STEP")
                .status(SagaStatus.PENDING)
                .timestamp(LocalDateTime.now())
                .payload(Map.of("bookingId", "999", "airbnbId", "1",
                        "checkInDate", "2025-01-01", "checkOutDate", "2025-01-05"))
                .build();

        redisTemplate.opsForList().rightPush(SAGA_QUEUE, objectMapper.writeValueAsString(event));
        return "Failing event pushed to saga queue with sagaId: " + event.getSagaId();
    }
}