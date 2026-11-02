package com.flybynt.booking_service.dto;

import jakarta.validation.constraints.NotBlank;
/** IDs come from validated Passenger and Seat selections, never from arbitrary CPR text. */
public record CreateBookingRequest(
        @NotBlank String flightId,
        @NotBlank String passengerId,
        @NotBlank String seatId, // Seat Service seat id (e.g. 6ae8735340adfcff685e4aca), not the seat number "1A"
        @NotBlank String idempotencyKey) {

}
