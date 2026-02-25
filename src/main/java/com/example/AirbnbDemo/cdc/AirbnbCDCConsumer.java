package com.example.AirbnbDemo.cdc;

import com.example.AirbnbDemo.models.readModels.AirbnbReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class AirbnbCDCConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "airbnb.airbnbspringdemo.airbnbs", groupId = "airbnb-cdc-group")
    public void consume(String message) {
        log.info("CDC received message: {}", message); // ← add this first
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode payload = root.path("payload");

            boolean deleted = "true".equals(payload.path("__deleted").stringValue());

            if (deleted) {
                String id = String.valueOf(payload.path("id").longValue());
                redisTemplate.delete("airbnb:" + id);
                log.info("CDC deleted airbnb {} from Redis", id);
                return;
            }

            AirbnbReadModel model = AirbnbReadModel.builder()
                    .id(payload.path("id").longValue())
                    .name(payload.path("name").stringValue())               // ← stringValue()
                    .description(payload.path("description").stringValue()) // ← stringValue()
                    .location(payload.path("location").stringValue())       // ← stringValue()
                    .pricePerNight(payload.path("price_per_night").longValue())
                    .build();

            redisTemplate.opsForValue().set(
                    "airbnb:" + model.getId(),
                    objectMapper.writeValueAsString(model)
            );
            log.info("CDC synced airbnb {} to Redis", model.getId());

        } catch (Exception e) {
            log.error("Failed to process airbnb CDC event: {}", e.getMessage(), e);
        }
    }
}