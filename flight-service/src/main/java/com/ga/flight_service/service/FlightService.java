package com.ga.flight_service.service;

import com.ga.flight_service.model.Aircraft;
import com.ga.flight_service.model.Airline;
import com.ga.flight_service.model.Airport;
import com.ga.flight_service.model.Flight;
import com.ga.flight_service.model.response.AllFlightResponse;
import com.ga.flight_service.model.response.FlightResponse;
import com.ga.flight_service.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightService {
    @Value("${airlabs.api.key}")
    private String apiKey;

    @Value("${airlabs.api.url}")
    private String apiUrl;

    private final FlightRepository flightRepository;

    //communicate with API
    private final RestClient restClient= RestClient.create();

    public List<Flight> getFlights(String departureAirport){
        //send a get request
        AllFlightResponse result= restClient.get()
                //AirLabs URL
                .uri(apiUrl+"/flights?dep_iata={airport}&api_key={key}", departureAirport,apiKey)
                //Retrieve the HTTP response
                .retrieve()
                //read the JSON response as String
                .body(AllFlightResponse.class);

        if (result== null || result.getResponse()==null){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"No flight data received");
        }

        List<Flight> flights= new ArrayList<>();

        for (FlightResponse response: result.getResponse()){
            if (response.getFlightIata()== null || response.getDepIata()== null){
                continue;
            }
            //if its exist in the database use it otherwise create a new object
            Flight flight= flightRepository.findByFlightNumberAndDepartureIataCode(response.getFlightIata(), response.getDepIata()).orElseGet(Flight::new);
            flight.setFlightNumber(response.getFlightIata());
            flight.setStatus(response.getStatus());

            Airport departure = new Airport();
            departure.setIataCode(response.getDepIata());
            flight.setDeparture(departure);

            Airport arrival = new Airport();
            arrival.setIataCode(response.getArrIata());
            flight.setArrival(arrival);

            Airline airline = new Airline();
            airline.setIataCode(response.getAirlineIata());
            flight.setAirline(airline);

            Aircraft aircraft = new Aircraft();
            aircraft.setIcaoCode(response.getAircraftIcao());
            flight.setAircraft(aircraft);

            flights.add(flightRepository.save(flight));
        }
        return flights;
    }

    public Flight getFlightById(String id){
        return flightRepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"Flight not found"));
    }
}
