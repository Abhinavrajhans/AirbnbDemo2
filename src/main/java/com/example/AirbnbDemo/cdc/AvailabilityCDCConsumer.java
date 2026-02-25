package com.example.AirbnbDemo.cdc;

import com.example.AirbnbDemo.Mapper.AvailabilityMapper;
import com.example.AirbnbDemo.models.readModels.AvailabilityReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;


@Component
@RequiredArgsConstructor
@Slf4j
public class AvailabilityCDCConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "airbnb.airbnbspringdemo.availabilities", groupId = "airbnb-cdc-group")
    public void consume(String message) {
        log.info("CDC received availability message: {}", message);
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode payload = root.path("payload");

            boolean deleted = "true".equals(payload.path("__deleted").stringValue());
            if (deleted) {
                log.info("CDC availability delete event received");
                return;
            }
            Long airbnbId = payload.path("airbnb_id").longValue();
            // ← Debezium stores date as epoch days (integer), convert to LocalDate string
            int epochDays = payload.path("date").intValue();
            String date = LocalDate.ofEpochDay(epochDays).toString(); // → "2026-02-25"
            AvailabilityReadModel model = AvailabilityMapper.toReadModelFromCDC(airbnbId,date,payload);
            redisTemplate.opsForHash().put(
                    "airbnb:availability:" + airbnbId,
                    date,
                    objectMapper.writeValueAsString(model)
            );
            log.info("CDC synced availability for airbnb {} date {}", airbnbId, date);

        } catch (Exception e) {
            log.error("Failed to process availability CDC event: {}", e.getMessage(), e);
        }
    }
}