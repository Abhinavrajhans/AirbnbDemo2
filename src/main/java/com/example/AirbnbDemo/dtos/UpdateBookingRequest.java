package com.example.AirbnbDemo.dtos;

import com.example.AirbnbDemo.models.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBookingRequest {

    @NotNull(message="Booking Id is Required")
    private Long id;

    @NotNull(message="Idempotency key is Required")
    private String idempotencyKey;

    @NotNull(message="Booking status is Required")
    private BookingStatus bookingStatus;

}
