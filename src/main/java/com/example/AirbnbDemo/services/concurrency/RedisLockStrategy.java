package com.example.AirbnbDemo.services.concurrency;

import com.example.AirbnbDemo.models.Availability;
import com.example.AirbnbDemo.repository.writes.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisLockStrategy implements ConcurrencyControlStrategy {

    private static final String LOCK_KEY_PREFIX="lock:availability:";
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5); // make this configurable

    private final RedisTemplate<String,String> redisTemplate;
    private final AvailabilityRepository availabilityRepository;

    @Override
    public void releaseLock(Long airbnbId, LocalDate checkIn, LocalDate checkOut) {
        String key = generateLockKey(airbnbId, checkIn, checkOut);
        redisTemplate.delete(key);
    }

    @Override
    public List<Availability> lockAndCheckAvailability(Long airbnbId, LocalDate checkInDate, LocalDate checkOutDate,Long userId) {
        Long bookedSlots=availabilityRepository.countByAirbnbIdAndDateBetweenAndBookingIsNotNull(airbnbId,checkInDate,checkOutDate);
        if(bookedSlots>0){
            throw new RuntimeException("Airbnb is not available for the given dates. Please try again with different dates.");
        }
        String lockKey=generateLockKey(airbnbId,checkInDate,checkOutDate);
        boolean locked=redisTemplate.opsForValue().setIfAbsent(lockKey,userId.toString(),LOCK_TIMEOUT);
        if(!locked){
            throw new IllegalStateException("Failed to acquire booking for the given dates. Please try again.");
        }
        try{
            return availabilityRepository.findByAirbnbIdAndDateBetween(airbnbId,checkInDate,checkOutDate);
        }
        catch (Exception e){
            releaseLock(airbnbId,checkInDate,checkOutDate);
            throw e;
        }
    }

    private String generateLockKey(Long airbnbId, LocalDate checkInDate, LocalDate checkOutDate) {
        return LOCK_KEY_PREFIX + airbnbId + checkInDate + checkOutDate;
    }

}
