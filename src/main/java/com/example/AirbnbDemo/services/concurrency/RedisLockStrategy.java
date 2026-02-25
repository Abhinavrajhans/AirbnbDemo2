package com.example.AirbnbDemo.services.concurrency;

import com.example.AirbnbDemo.models.Availability;
import com.example.AirbnbDemo.repository.writes.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockStrategy implements ConcurrencyControlStrategy {

    private static final String LOCK_KEY_PREFIX="lock:availability:";
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5); // make this configurable

    private final RedisTemplate<String,String> redisTemplate;
    private final AvailabilityRepository availabilityRepository;

    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +    // owner matches → delete
                    "else " +
                    "    return 0 " +                              // not owner → do nothing
                    "end";

    public void releaseLock(Long airbnbId, LocalDate checkIn, LocalDate checkOut, Long userId) {
        String key = generateLockKey(airbnbId, checkIn, checkOut);
        redisTemplate.execute(
                new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class),
                List.of(key),        // KEYS[1]
                userId.toString()    // ARGV[1]
        );
    }

    @Override
    public List<Availability> lockAndCheckAvailability(Long airbnbId, LocalDate checkInDate, LocalDate checkOutDate,Long userId) {
        String lockKey=generateLockKey(airbnbId,checkInDate,checkOutDate);
        Boolean locked=redisTemplate.opsForValue().setIfAbsent(lockKey,userId.toString(),LOCK_TIMEOUT);
        if (!Boolean.TRUE.equals(locked)) {
            throw new IllegalStateException("Failed to acquire booking for the given dates. Please try again.");
        }
        try{
            Long bookedSlots=availabilityRepository.countByAirbnbIdAndDateBetweenAndBookingIsNotNull(airbnbId,checkInDate,checkOutDate);
            if(bookedSlots>0){
                releaseLock(airbnbId,checkInDate,checkOutDate,userId);
                throw new RuntimeException("Airbnb is not available for the given dates. Please try again with different dates.");
            }
            return availabilityRepository.findByAirbnbIdAndDateBetween(airbnbId,checkInDate,checkOutDate);
        }
        catch (Exception e){
            releaseLock(airbnbId,checkInDate,checkOutDate,userId);
            throw e;
        }
    }

    private String generateLockKey(Long airbnbId, LocalDate checkInDate, LocalDate checkOutDate) {
        return LOCK_KEY_PREFIX + airbnbId +":"+checkInDate +":"+checkOutDate;
    }

}
