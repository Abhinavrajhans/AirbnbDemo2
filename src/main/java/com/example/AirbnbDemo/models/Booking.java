package com.example.AirbnbDemo.models;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_booking_user_id",   columnList = "user_id"),
        @Index(name = "idx_booking_airbnb_id", columnList = "airbnb_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseModel {

    // Many bookings belong to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Many bookings belong to one Airbnb
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airbnb_id", nullable = false)
    private Airbnb airbnb;

    @Column(nullable = false)
    private double totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    public String toString(){
        return idempotencyKey;
    }

}
