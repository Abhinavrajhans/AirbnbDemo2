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
        try {
            JsonNode root = objectMapper.readTree(message);
            String op = root.path("__op").toString();
            boolean deleted = root.path("__deleted").toString().equals("true");

            if (deleted || "d".equals(op)) {
                String id = root.path("id").toString();
                redisTemplate.delete("airbnb:" + id);
                log.info("CDC deleted airbnb {} from Redis", id);
                return;
            }

            AirbnbReadModel model = AirbnbReadModel.builder()
                    .id(Long.parseLong(root.path("id").toString()))
                    .name(root.path("name").toString())
                    .description(root.path("description").toString())
                    .location(root.path("location").toString())
                    .pricePerNight(Long.parseLong(root.path("price_per_night").toString()))
                    .build();


            redisTemplate.opsForValue().set(
                    "airbnb:" + model.getId(),
                    objectMapper.writeValueAsString(model)
            );
            log.info("CDC synced airbnb {} to Redis (op={})", model.getId(), op);

        } catch (Exception e) {
            log.error("Failed to process airbnb CDC event: {}", e.getMessage(), e);
        }
    }
}