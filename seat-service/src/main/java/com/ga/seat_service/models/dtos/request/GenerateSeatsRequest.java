package com.ga.seat_service.models.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class GenerateSeatsRequest {
    @NotEmpty
    @Valid
    private List<SeatSectionRequest> sections;
}
