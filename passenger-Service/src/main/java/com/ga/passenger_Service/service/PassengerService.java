package com.ga.passenger_Service.service;

import com.ga.passenger_Service.dto.PassengerRequest;
import com.ga.passenger_Service.dto.PassengerResponse;
import com.ga.passenger_Service.model.Passenger;
import com.ga.passenger_Service.repository.PassengerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PassengerService {

    private final PassengerRepository passengerRepository;

    public PassengerService(PassengerRepository passengerRepository) {
        this.passengerRepository = passengerRepository;
    }

}