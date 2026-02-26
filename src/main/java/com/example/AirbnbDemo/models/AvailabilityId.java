package com.example.AirbnbDemo.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityId implements Serializable {
    // Field names must exactly match the @Id field names in Availability.
    // For a @ManyToOne @Id field, the type here is the PK type of the referenced entity (Long).
    private Long airbnb;
    private LocalDate date;
}
