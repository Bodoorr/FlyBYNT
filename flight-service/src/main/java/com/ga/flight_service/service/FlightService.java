package com.ga.flight_service.service;

import com.ga.flight_service.model.Flight;
import com.ga.flight_service.repository.FlightRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class FlightService {
    @Value("${airlabs.api.key}")
    private String apiKey;

    @Value("${airlabs.api.url}")
    private String apiUrl;

    private final FlightRepository flightRepository;

    //communicate with API
    private final RestClient restClient= RestClient.create();

    public String getFlights(String departureAirport){
        //send a get request
        return restClient.get()
                //AirLabs URL
                .uri(apiUrl+"/flights?dep_iata={airport}&api_key={key}", departureAirport,apiKey)
                //Retrieve the HTTP response
                .retrieve()
                //read the JSON response as String
                .body(String.class);
    }

    public Flight getFlightById(String id){
        return flightRepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"Flight not found"));
    }
}
