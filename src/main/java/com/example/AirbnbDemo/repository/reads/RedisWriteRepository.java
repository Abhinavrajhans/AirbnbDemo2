package com.example.AirbnbDemo.repository.reads;

import com.example.AirbnbDemo.mapper.AirbnbMapper;
import com.example.AirbnbDemo.mapper.AvailabilityMapper;
import com.example.AirbnbDemo.mapper.BookingMapper;
import com.example.AirbnbDemo.models.Airbnb;
import com.example.AirbnbDemo.models.Availability;
import com.example.AirbnbDemo.models.Booking;
import com.example.AirbnbDemo.models.readModels.AirbnbReadModel;
import com.example.AirbnbDemo.models.readModels.AvailabilityReadModel;
import com.example.AirbnbDemo.models.readModels.BookingReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RedisWriteRepository {

        private final RedisTemplate<String, String> redisTemplate;
        private final ObjectMapper objectMapper;

        public void writeBooking(Booking booking) {
            BookingReadModel model = BookingMapper.toReadModel(booking);
            save(RedisReadRepository.BOOKING_KEY_PREFIX + model.getId(), model);
            save(RedisReadRepository.IDEMPOTENCY_KEY_PREFIX + model.getIdempotencyKey(), model.getId());
        }

        // Write a single availability slot into the airbnb's hash
        public void writeAvailability(Availability availability) {
            AvailabilityReadModel model = AvailabilityMapper.toReadModel(availability);
            String hashKey = RedisReadRepository.AIRBNB_AVAILABILITY_PREFIX + model.getAirbnbId();
            String field = model.getDate(); // "2025-01-01"
            try {
                redisTemplate.opsForHash().put(hashKey, field, objectMapper.writeValueAsString(model));
            } catch (JacksonException e) {
                throw new RuntimeException("Failed to serialize availability", e);
            }
        }

        public void writeAvailabilities(Long airbnbId, List<Availability> availabilities) {
            if (availabilities == null || availabilities.isEmpty()) return;
            String hashKey = RedisReadRepository.AIRBNB_AVAILABILITY_PREFIX + airbnbId;
            for (Availability a : availabilities) {
                AvailabilityReadModel model = AvailabilityMapper.toReadModel(a);
                try {
                    redisTemplate.opsForHash().put(hashKey, model.getDate(), objectMapper.writeValueAsString(model));
                } catch (JacksonException e) {
                    throw new RuntimeException("Failed to serialize availability for airbnb " + airbnbId, e);
                }
            }
        }


        public void writeAirbnb(Airbnb airbnb) {
            AirbnbReadModel model = AirbnbMapper.toReadModel(airbnb);
            save(RedisReadRepository.AIRBNB_KEY_PREFIX + airbnb.getId(), model);
        }

        public void deleteAirbnb(Long id) {
            String key=RedisReadRepository.AIRBNB_KEY_PREFIX + id;
            delete(key);
        }

        private void delete(String key){
            redisTemplate.delete(key);
        }

        private void save(String key, Object value) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value));
            } catch (JacksonException e) {
                throw new RuntimeException("Failed to serialize " + value.getClass().getSimpleName() + " to Redis", e);
            }
        }


    // Delete a single date field from the hash
//    public void deleteAvailabilityDate(Long airbnbId, String date) {
//        redisTemplate.opsForHash().delete(AIRBNB_AVAILABILITY_PREFIX + airbnbId, date);
//    }

}
