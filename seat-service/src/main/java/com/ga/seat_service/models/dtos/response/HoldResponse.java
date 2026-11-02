package com.ga.seat_service.models.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HoldResponse {
    private String flightId;
    private String seatId;
    private String holdToken;
    private Instant holdExpiresAt;
}
