package com.flybynt.booking_service.dto;

import jakarta.validation.constraints.NotBlank;
/** IDs come from validated Passenger and Seat selections, never from arbitrary CPR text. */
public record CreateBookingRequest(
        @NotBlank String flightId,
        @NotBlank String passengerId,
        @NotBlank String seatId,
        @NotBlank String idempotencyKey) {

}
