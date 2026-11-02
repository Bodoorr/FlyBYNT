package com.ga.seat_service.exceptions;

public class SeatUnAvailableException extends RuntimeException {
    public SeatUnAvailableException(String message) {
        super(message);
    }
}
