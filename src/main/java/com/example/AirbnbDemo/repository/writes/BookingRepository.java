package com.example.AirbnbDemo.repository.writes;

import com.example.AirbnbDemo.models.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long id);
    List<Booking> findByAirbnbId(Long id);
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);
}
