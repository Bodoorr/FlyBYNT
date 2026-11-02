package com.ga.seat_service.models.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SeatRequest {
    @NotBlank
    private String flightId;

    @NotBlank
    private String seatId;
}
