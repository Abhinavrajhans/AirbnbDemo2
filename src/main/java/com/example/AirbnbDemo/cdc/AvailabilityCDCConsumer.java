package com.example.AirbnbDemo.cdc;

import com.example.AirbnbDemo.models.readModels.AvailabilityReadModel;
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
public class AvailabilityCDCConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "airbnb.airbnbspringdemo.availabilities", groupId = "airbnb-cdc-group")
    public void consume(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String op = root.path("__op").toString();
            Long airbnbId = root.path("airbnb_id").asLong();
            String date = root.path("date").toString();

            AvailabilityReadModel model = AvailabilityReadModel.builder()
                    .id(Long.parseLong(root.path("id").toString()))
                    .airbnbId(airbnbId)
                    .date(date)
                    .bookingId(root.path("booking_id").isNull() ? null : Long.parseLong(root.path("booking_id").toString()))
                    .isAvailable(Boolean.parseBoolean(root.path("is_available").toString()))
                    .build();

            redisTemplate.opsForHash().put(
                    "airbnb:availability:" + airbnbId,
                    date,
                    objectMapper.writeValueAsString(model)
            );
            log.info("CDC synced availability for airbnb {} date {} (op={})", airbnbId, date, op);

        } catch (Exception e) {
            log.error("Failed to process availability CDC event: {}", e.getMessage(), e);
        }
    }
}