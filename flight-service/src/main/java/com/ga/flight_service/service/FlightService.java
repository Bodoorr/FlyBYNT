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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter AIRLABS_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final BigDecimal BASE_FARE = new BigDecimal("30");
    private static final BigDecimal PER_MINUTE = new BigDecimal("0.25");

    //communicate with API
    private final RestClient restClient= RestClient.create();

    public List<Flight> getFlights(String departureAirport){
        //send a get request
        AllFlightResponse result= restClient.get()
                //AirLabs URL
                // schedules (not live /flights) so upcoming flights come with departure/arrival times
                .uri(apiUrl+"/schedules?dep_iata={airport}&api_key={key}", departureAirport,apiKey)
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
            flight.setDepartureTime(parseUtc(response.getDepTimeUtc()));
            flight.setArrivalTime(parseUtc(response.getArrTimeUtc()));
            flight.setPrice(priceFor(flight, response.getDuration()));

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

    // AirLabs gives UTC times as "yyyy-MM-dd HH:mm"
    private static Instant parseUtc(String time){
        if (time == null) {
            return null;
        }
        return LocalDateTime.parse(time, AIRLABS_TIME).toInstant(ZoneOffset.UTC);
    }

    // AirLabs has no fares. Placeholder: base fare + per-minute rate until the team decides real pricing
    private static BigDecimal priceFor(Flight flight, Integer durationMinutes){
        Integer minutes = durationMinutes;
        if (minutes == null && flight.getDepartureTime() != null && flight.getArrivalTime() != null) {
            minutes = (int) Duration.between(flight.getDepartureTime(), flight.getArrivalTime()).toMinutes();
        }
        if (minutes == null) {
            return null;
        }
        return BASE_FARE.add(PER_MINUTE.multiply(BigDecimal.valueOf(minutes))).setScale(2, RoundingMode.HALF_UP);
    }

    public Flight getFlightById(String id){
        return flightRepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"Flight not found"));
    }
}
