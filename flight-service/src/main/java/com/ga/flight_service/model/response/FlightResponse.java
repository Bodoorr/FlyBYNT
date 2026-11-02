package com.ga.flight_service.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightResponse {
    private String flightIata;
    private String depIata;
    private String arrIata;
    private String airlineIata;
    private String aircraftIcao;
    private String status;
}
