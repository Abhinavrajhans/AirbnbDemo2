package com.example.AirbnbDemo.repository.writes;

import com.example.AirbnbDemo.models.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByAirbnbId(Long airbnbId);
    List<Availability> findByAirbnbIdAndDateBetween(Long airbnbId, LocalDate startDate, LocalDate endDate);
}

