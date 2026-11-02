package com.ga.passenger_Service.service;

import com.ga.passenger_Service.dto.PassengerRequest;
import com.ga.passenger_Service.dto.PassengerResponse;
import com.ga.passenger_Service.model.Passenger;
import com.ga.passenger_Service.repository.PassengerRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class PassengerService {

    private final PassengerRepository passengerRepository;

    public PassengerService(PassengerRepository passengerRepository) {
        this.passengerRepository = passengerRepository;
    }

    public PassengerResponse createPassenger(
            PassengerRequest request, String userId) {

        Passenger passenger = new Passenger();

        passenger.setUserId(userId);
        passenger.setFirstName(request.getFirstName());
        passenger.setLastName(request.getLastName());
        passenger.setDateOfBirth(request.getDateOfBirth());
        passenger.setNationality(request.getNationality());
        passenger.setPassportNumber(request.getPassportNumber());
        passenger.setCreatedAt(Instant.now());

        Passenger savedPassenger = passengerRepository.save(passenger);

        PassengerResponse response = new PassengerResponse();

        response.setId(savedPassenger.getId());
        response.setFirstName(savedPassenger.getFirstName());
        response.setLastName(savedPassenger.getLastName());
        response.setDateOfBirth(savedPassenger.getDateOfBirth());
        response.setNationality(savedPassenger.getNationality());
        response.setCreatedAt(savedPassenger.getCreatedAt());

        return response;
    }

    public List<PassengerResponse> getMyPassengers(String userId) {

        List<Passenger> passengers = passengerRepository.findByUserId(userId);

        return passengers.stream().map(this::toResponse)
                .toList();
    }

    private PassengerResponse toResponse(Passenger passenger) {

        PassengerResponse response = new PassengerResponse();

        response.setId(passenger.getId());
        response.setFirstName(passenger.getFirstName());
        response.setLastName(passenger.getLastName());
        response.setDateOfBirth(passenger.getDateOfBirth());
        response.setNationality(passenger.getNationality());
        response.setCreatedAt(passenger.getCreatedAt());

        return response;
    }

}