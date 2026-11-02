package com.ga.flight_service.repository;

import com.ga.flight_service.model.Flight;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FlightRepository extends MongoRepository<Flight, String> {
    Optional<Flight> findByFlightNumberAndDepartureIataCode(
            String flightNumber,
            String departureIataCode
    );
}
