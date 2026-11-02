package com.flybynt.booking_service.model;

/** Lifecycle of an individual passenger booking. */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    FAILED,
    CANCELLED
}
