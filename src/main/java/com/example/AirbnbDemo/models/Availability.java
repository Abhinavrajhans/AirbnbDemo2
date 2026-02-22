package com.example.AirbnbDemo.models;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "availabilities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Availability extends BaseModel {

    // Many availability slots belong to one Airbnb
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airbnb_id", nullable = false)
    private Airbnb airbnb;

    @Column(nullable = false)
    private LocalDate date;

    // Many availability slots can reference one booking (when booked)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking; // null if available

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;
}