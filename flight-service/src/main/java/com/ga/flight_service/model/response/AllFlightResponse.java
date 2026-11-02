package com.ga.flight_service.model.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AllFlightResponse {
    private List<FlightResponse> response;
}
