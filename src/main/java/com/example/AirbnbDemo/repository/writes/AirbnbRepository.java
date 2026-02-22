package com.example.AirbnbDemo.repository.writes;

import com.example.AirbnbDemo.models.Airbnb;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AirbnbRepository extends JpaRepository<Airbnb, Long> {
}
