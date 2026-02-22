package com.example.AirbnbDemo.exceptions;

public class UserEmailAlreadyExistsException extends RuntimeException{
    public UserEmailAlreadyExistsException(String message){
        super(message);
    }
}
