package com.ga.flight_service.controller;

import com.ga.flight_service.service.FlightService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/flights")
@AllArgsConstructor
public class FlightController {
    private final FlightService flightService;

    @GetMapping
    public String getFlights(@RequestParam String departure){
        return flightService.getFlights(departure);
    }
}
