package com.ga.flight_service.controller;

import com.ga.flight_service.model.Flight;
import com.ga.flight_service.service.FlightService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/flights")
@AllArgsConstructor
public class FlightController {
    private final FlightService flightService;

    @GetMapping
    public String getFlights(@RequestParam String departure){
        return flightService.getFlights(departure);
    }

    @GetMapping("/{id}")
    public Flight getFlightById(@PathVariable String id){
        return flightService.getFlightById(id);
    }
}
