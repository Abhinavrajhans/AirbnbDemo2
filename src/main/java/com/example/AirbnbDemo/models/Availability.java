package com.example.AirbnbDemo.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "availabilities")
@IdClass(AvailabilityId.class)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Availability {

    // ── Composite Primary Key ──────────────────────────────────────
    // (airbnb_id, date) uniquely identifies one availability slot.
    // This enforces at the DB level that no two rows exist for the
    // same property on the same date.

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airbnb_id", nullable = false)
    private Airbnb airbnb;

    @Id
    @Column(nullable = false)
    private LocalDate date;

    // ── Other columns ──────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking; // null if the slot is available

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    // ── Audit fields (replaces BaseModel since we have a custom PK) ─

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
