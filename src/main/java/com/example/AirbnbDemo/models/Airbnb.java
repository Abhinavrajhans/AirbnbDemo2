package com.example.AirbnbDemo.models;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "airbnbs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airbnb extends BaseModel {

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Long pricePerNight;

    @Column(nullable = false)
    private String location;

    // One Airbnb can have many bookings
    @OneToMany(mappedBy = "airbnb", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    // One Airbnb can have many availability slots
    @OneToMany(mappedBy = "airbnb", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Availability> availabilities = new ArrayList<>();
}
