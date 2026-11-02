package com.ga.seat_service.exceptions;

public class SeatsAlreadyExistException extends RuntimeException {
    public SeatsAlreadyExistException(String message) {
        super(message);
    }
}
