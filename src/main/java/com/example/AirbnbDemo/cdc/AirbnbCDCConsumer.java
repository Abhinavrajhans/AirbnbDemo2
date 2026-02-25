package com.example.AirbnbDemo.cdc;

import com.example.AirbnbDemo.Mapper.AirbnbMapper;
import com.example.AirbnbDemo.models.readModels.AirbnbReadModel;
import com.example.AirbnbDemo.repository.reads.RedisReadRepository;
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
                redisTemplate.delete(RedisReadRepository.AIRBNB_KEY_PREFIX + id);
                log.info("CDC deleted airbnb {} from Redis", id);
                return;
            }

            AirbnbReadModel model = AirbnbMapper.toReadModelFromCDC(payload);

            redisTemplate.opsForValue().set(
                    RedisReadRepository.AIRBNB_KEY_PREFIX + model.getId(),
                    objectMapper.writeValueAsString(model)
            );
            log.info("CDC synced airbnb {} to Redis", model.getId());

        } catch (Exception e) {
            log.error("Failed to process airbnb CDC event: {}", e.getMessage(), e);
        }
    }
}