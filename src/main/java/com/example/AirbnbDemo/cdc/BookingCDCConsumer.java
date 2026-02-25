package com.example.AirbnbDemo.cdc;

import com.example.AirbnbDemo.models.readModels.BookingReadModel;
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
public class BookingCDCConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "airbnb.airbnbspringdemo.bookings", groupId = "airbnb-cdc-group")
    public void consume(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String op = root.path("__op").toString();

            Long id = root.path("id").asLong();
            String idempotencyKey = root.path("idempotency_key").toString();

            BookingReadModel model = BookingReadModel.builder()
                    .id(id)
                    .userId(Long.parseLong(root.path("user_id").toString()))
                    .airbnbId(Long.parseLong(root.path("airbnb_id").toString()))
                    .totalPrice(Double.parseDouble(root.path("total_price").toString()))
                    .bookingStatus(root.path("status").toString())
                    .idempotencyKey(idempotencyKey)
                    .checkInDate(LocalDate.parse(root.path("check_in_date").toString()))
                    .checkOutDate(LocalDate.parse(root.path("check_out_date").toString()))
                    .build();

            redisTemplate.opsForValue().set(
                    "booking:" + id,
                    objectMapper.writeValueAsString(model)
            );
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                redisTemplate.opsForValue().set("idempotency:" + idempotencyKey, id.toString());
            }
            log.info("CDC synced booking {} to Redis (op={})", id, op);

        } catch (Exception e) {
            log.error("Failed to process booking CDC event: {}", e.getMessage(), e);
        }
    }
}