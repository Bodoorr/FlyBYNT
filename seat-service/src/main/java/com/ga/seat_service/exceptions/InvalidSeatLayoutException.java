package com.ga.seat_service.exceptions;

public class InvalidSeatLayoutException extends RuntimeException {
    public InvalidSeatLayoutException(String message) {
        super(message);
    }
}
