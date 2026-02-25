package com.example.AirbnbDemo.cdc;

import com.example.AirbnbDemo.mapper.BookingMapper;
import com.example.AirbnbDemo.models.readModels.BookingReadModel;
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
public class BookingCDCConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "airbnb.airbnbspringdemo.bookings", groupId = "airbnb-cdc-group")
    public void consume(String message) {
        log.info("CDC received booking message: {}", message);
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode payload = root.path("payload");

            boolean deleted = "true".equals(payload.path("__deleted").stringValue());
            if (deleted) {
                log.info("CDC booking delete event received");
                return;
            }
            Long id = payload.path("id").longValue();
            String idempotencyKey = payload.path("idempotency_key").stringValue();
            BookingReadModel model = BookingMapper.ToReadModelFromCDC(id,idempotencyKey,payload);
            redisTemplate.opsForValue().set(
                    RedisReadRepository.BOOKING_KEY_PREFIX + id,
                    objectMapper.writeValueAsString(model)
            );
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                redisTemplate.opsForValue().set(
                        RedisReadRepository.IDEMPOTENCY_KEY_PREFIX + idempotencyKey, id.toString());
                log.info("CDC stored idempotency key {} → booking {}", idempotencyKey, id);
            }
            log.info("CDC synced booking {} to Redis", id);

        } catch (Exception e) {
            log.error("Failed to process booking CDC event: {}", e.getMessage(), e);
        }
    }
}