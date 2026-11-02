package com.ga.passenger_Service.service;

import com.ga.passenger_Service.dto.PassengerRequest;
import com.ga.passenger_Service.dto.PassengerResponse;
import com.ga.passenger_Service.exception.PassengerNotFoundException;
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
    // Creates a new passenger and links it to the user and saves it in MongoDB
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

        return toResponse(savedPassenger);
    }

    // Retrieves all passengers belonging to a specific user
    public List<PassengerResponse> getMyPassengers(String userId) {

        List<Passenger> passengers = passengerRepository.findByUserId(userId);

        return passengers.stream().map(this::toResponse)
                .toList();
    }
    // Finds a passenger by ID and checks that the user owns the passenger
    public PassengerResponse getPassengerById(String passengerId, String userId) {

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new PassengerNotFoundException("Passenger not found"));

        if (!passenger.getUserId().equals(userId)) {
            throw new PassengerNotFoundException("Passenger not found");
        }

        return toResponse(passenger);
    }
    // Updates a passengers information after verifying ownership
    public PassengerResponse updatePassenger(
            String passengerId, PassengerRequest request, String userId) {

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new PassengerNotFoundException("Passenger not found"));

        if (!passenger.getUserId().equals(userId)) {
            throw new PassengerNotFoundException("Passenger not found");
        }

        passenger.setFirstName(request.getFirstName());
        passenger.setLastName(request.getLastName());
        passenger.setDateOfBirth(request.getDateOfBirth());
        passenger.setNationality(request.getNationality());
        passenger.setPassportNumber(request.getPassportNumber());

        Passenger updatedPassenger = passengerRepository.save(passenger);

        return toResponse(updatedPassenger);
    }
    // Deletes a passenger only if the passenger belongs to the user
    public void deletePassenger(String passengerId, String userId) {

        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new PassengerNotFoundException("Passenger not found"));

        if (!passenger.getUserId().equals(userId)) {
            throw new PassengerNotFoundException("Passenger not found");
        }

        passengerRepository.delete(passenger);
    }


    // Converts a Passenger model into a response without exposing the passport number
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