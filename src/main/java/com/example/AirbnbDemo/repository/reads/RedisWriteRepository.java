package com.example.AirbnbDemo.repository.reads;

import com.example.AirbnbDemo.Mapper.AirbnbMapper;
import com.example.AirbnbDemo.Mapper.AvailabilityMapper;
import com.example.AirbnbDemo.Mapper.BookingMapper;
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
        }

        public void writeAvailability(Availability availability) {
            AvailabilityReadModel model = AvailabilityMapper.toReadModel(availability);
            save(RedisReadRepository.AVAILABLE_KEY_PREFIX + model.getId(), model);
        }

        public void writeAirbnb(Airbnb airbnb, List<AvailabilityReadModel> availabilities) {
            AirbnbReadModel model = AirbnbMapper.toReadModel(airbnb, availabilities);
            save(RedisReadRepository.AIRBNB_KEY_PREFIX + airbnb.getId(), model);
        }

        private void save(String key, Object value) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value));
            } catch (JacksonException e) {
                throw new RuntimeException("Failed to serialize " + value.getClass().getSimpleName() + " to Redis", e);
            }
        }

}
