package com.example.AirbnbDemo.exceptions;

public class AvailabilityAlreadyExistsException extends RuntimeException {
    public AvailabilityAlreadyExistsException(String message) {
        super(message);
    }
}
