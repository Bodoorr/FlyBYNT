package com.ga.seat_service.models.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

// what booking-service reads back from POST /seats/hold
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingHoldResponse {
    private boolean held;
    private String holdToken;
    private Instant holdExpiresAt;
}
