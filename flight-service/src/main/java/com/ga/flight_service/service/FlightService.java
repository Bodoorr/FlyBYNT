package com.ga.flight_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FlightService {
    @Value("${airlabs.api.key}")
    private String apiKey;

    @Value("${airlabs.api.url}")
    private String apiUrl;

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
}
