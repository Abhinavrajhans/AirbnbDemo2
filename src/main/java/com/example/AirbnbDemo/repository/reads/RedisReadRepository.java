package com.example.AirbnbDemo.repository.reads;

import com.example.AirbnbDemo.models.readModels.AirbnbReadModel;
import com.example.AirbnbDemo.models.readModels.AvailabilityReadModel;
import com.example.AirbnbDemo.models.readModels.BookingReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
@Repository
@RequiredArgsConstructor
public class RedisReadRepository {

    public static final String AIRBNB_KEY_PREFIX = "airbnb:";
    public static final String BOOKING_KEY_PREFIX = "booking:";
    public static final String AVAILABLE_KEY_PREFIX = "available:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public AirbnbReadModel getAirbnbById(Long id) {
        return getByKey(AIRBNB_KEY_PREFIX + id, AirbnbReadModel.class);
    }

    public BookingReadModel getBookingById(Long id) {
        return getByKey(BOOKING_KEY_PREFIX + id, BookingReadModel.class);
    }

    public AvailabilityReadModel getAvailabilityById(Long id) {
        return getByKey(AVAILABLE_KEY_PREFIX + id, AvailabilityReadModel.class);
    }

    public List<AirbnbReadModel> getAllAirbnbs() {
        return getAllByPrefix(AIRBNB_KEY_PREFIX, AirbnbReadModel.class);
    }

    public BookingReadModel findBookingByIdempotencyKey(String idempotencyKey) {
        return getAllByPrefix(BOOKING_KEY_PREFIX, BookingReadModel.class).stream()
                .filter(m -> idempotencyKey.equals(m.getIdempotencyKey()))
                .findFirst()
                .orElse(null);
    }

    // ─── private helpers ───────────────────────────────────────────

    private <T> T getByKey(String key, Class<T> type) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, type);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to parse " + type.getSimpleName() + " from Redis", e);
        }
    }

    private <T> List<T> getAllByPrefix(String prefix, Class<T> type) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys == null || keys.isEmpty()) return List.of();
        return keys.stream()
                .map(k -> getByKey(k, type))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}