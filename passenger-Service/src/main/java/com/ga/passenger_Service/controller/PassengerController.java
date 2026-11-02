package com.ga.passenger_Service.controller;

import com.ga.passenger_Service.service.PassengerService;
import org.springframework.web.bind.annotation.*;

import com.ga.passenger_Service.dto.PassengerRequest;
import com.ga.passenger_Service.dto.PassengerResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/passengers")
public class PassengerController {

    private final PassengerService passengerService;

    public PassengerController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    // Creates a new passenger for the authenticated user
    @PostMapping
    public ResponseEntity<PassengerResponse> createPassenger(@Valid @RequestBody PassengerRequest request,
            Principal principal) {

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String userId = principal.getName();

        PassengerResponse response = passengerService.createPassenger(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Retrieves all passengers belonging to the authenticated user.
    @GetMapping("/me")
    public ResponseEntity<List<PassengerResponse>> getMyPassengers(
            Principal principal) {

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String userId = principal.getName();
        List<PassengerResponse> passengers = passengerService.getMyPassengers(userId);
        return ResponseEntity.ok(passengers);
    }

    // Retrieves one passenger after checking ownership
    @GetMapping("/{id}")
    public ResponseEntity<PassengerResponse> getPassengerById(
            @PathVariable String id, Principal principal) {

        String userId = getAuthenticatedUserId(principal);

        PassengerResponse response = passengerService.getPassengerById(id, userId);

        return ResponseEntity.ok(response);
    }

    // Updates a passenger only if they belong to the authenticated user
    @PutMapping("/{id}")
    public ResponseEntity<PassengerResponse> updatePassenger(
            @PathVariable String id,
            @Valid @RequestBody PassengerRequest request,
            Principal principal) {

        String userId = getAuthenticatedUserId(principal);

        PassengerResponse response = passengerService.updatePassenger(id, request, userId);

        return ResponseEntity.ok(response);
    }

    // Deletes a passenger after verifying ownership.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePassenger(
            @PathVariable String id, Principal principal) {

        String userId = getAuthenticatedUserId(principal);

        passengerService.deletePassenger(id, userId);

        return ResponseEntity.noContent().build();
    }

    // Gets the user ID from the authenticated principal
    private String getAuthenticatedUserId(Principal principal) {

        if (principal == null || principal.getName() == null
                || principal.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return principal.getName();
    }
}