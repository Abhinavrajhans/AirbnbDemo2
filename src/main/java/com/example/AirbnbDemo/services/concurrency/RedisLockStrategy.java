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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockStrategy implements ConcurrencyControlStrategy {

    private static final String LOCK_KEY_PREFIX="lock:availability:";
    private static final String LOCK_KEY_UPDATE_PREFIX = "lock:booking:update:";
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5); // make this configurable

    private final RedisTemplate<String,String> redisTemplate;
    private final AvailabilityRepository availabilityRepository;


    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +    // owner matches → delete
                    "else " +
                    "    return 0 " +                              // not owner → do nothing
                    "end";

    private static final DefaultRedisScript<Long> RELEASE_REDIS_SCRIPT =
            new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);

    private void releaseAnyLock(String lockKey, String lockValue) {
        redisTemplate.execute(RELEASE_REDIS_SCRIPT, List.of(lockKey), lockValue);
    }

    @Override
    public void releaseBookingLock(Long airbnbId, LocalDate checkIn, LocalDate checkOut, Long userId) {
        String key = generateLockKey(airbnbId, checkIn, checkOut);
        releaseAnyLock(key,userId.toString());
    }

    @Override
    public void releaseUpdateBookingLock(Long bookingId,String value) {
        String key= LOCK_KEY_UPDATE_PREFIX+bookingId;
        releaseAnyLock(key,value);
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
            if(bookedSlots>0) throw new RuntimeException("Airbnb is not available for the given dates. Please try again with different dates.");
            return availabilityRepository.findByAirbnbIdAndDateBetween(airbnbId,checkInDate,checkOutDate);
        }
        catch (Exception e){
            releaseBookingLock(airbnbId,checkInDate,checkOutDate,userId);
            throw e;
        }
    }

    public String lockAndUpdateBooking(Long bookingId){
        if(bookingId==null) return null;
        String lockKey = generateUpdateLockKey(bookingId);
        String lockValue = UUID.randomUUID().toString();
        // Try to acquire lock — only ONE request wins
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10));
        if (!Boolean.TRUE.equals(locked)) return null;
        return lockValue;
    }

    private String generateUpdateLockKey(Long bookingId){
        return  LOCK_KEY_UPDATE_PREFIX + bookingId;
    }
    private String generateLockKey(Long airbnbId, LocalDate checkInDate, LocalDate checkOutDate) {
        return LOCK_KEY_PREFIX + airbnbId +":"+checkInDate +":"+checkOutDate;
    }

}
