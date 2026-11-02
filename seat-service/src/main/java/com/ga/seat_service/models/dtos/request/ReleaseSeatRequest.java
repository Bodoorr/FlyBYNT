package com.ga.seat_service.models.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReleaseSeatRequest {
    @NotBlank
    private String holdToken;
}
