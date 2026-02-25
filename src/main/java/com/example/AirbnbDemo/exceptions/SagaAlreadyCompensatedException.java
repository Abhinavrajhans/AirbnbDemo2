package com.example.AirbnbDemo.exceptions;

public class SagaAlreadyCompensatedException extends RuntimeException {
    public SagaAlreadyCompensatedException(String message) {
        super(message);
    }
}