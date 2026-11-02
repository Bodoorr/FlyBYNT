package com.ga.seat_service.models.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmSeatRequest {
    @NotBlank
    private String holdToken;

    @NotBlank
    private String bookingId;
}