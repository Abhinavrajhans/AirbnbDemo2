package com.example.AirbnbDemo.repository.writes;

import com.example.AirbnbDemo.models.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByAirbnbId(Long airbnbId);
    List<Availability> findByAirbnbIdAndDateBetween(Long airbnbId, LocalDate startDate, LocalDate endDate);
    Long countByAirbnbIdAndDateBetweenAndBookingIsNotNull(Long airbnbId, LocalDate startDate, LocalDate endDate);

    @Modifying
    @Transactional
    @Query("UPDATE Availability a SET a.booking.id = :bookingId, a.isAvailable = false " +
            "WHERE a.airbnb.id = :airbnbId AND a.date BETWEEN :startDate AND :endDate")
    void updateBookingIdByAirbnbIdAndDateBetween(
            @Param("bookingId") Long bookingId,
            @Param("airbnbId")  Long airbnbId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);


    @Modifying
    @Transactional
    @Query(value = "UPDATE availabilities SET booking_id = NULL, is_available = true " +
            "WHERE airbnb_id = :airbnbId AND date BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    void clearBookingByAirbnbIdAndDateBetween(
            @Param("airbnbId")  Long airbnbId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);
}

