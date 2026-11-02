package com.ga.seat_service.models.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// body booking-service sends to POST /seats/hold
@Data
public class BookingHoldRequest {
    @NotBlank
    private String flightId;

    @NotBlank
    private String seatId;

    @NotBlank
    private String bookingId;
}
