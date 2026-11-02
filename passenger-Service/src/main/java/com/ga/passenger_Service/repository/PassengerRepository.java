package com.ga.passenger_Service.repository;

import com.ga.passenger_Service.model.Passenger;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PassengerRepository extends MongoRepository<Passenger, String> {

    List<Passenger> findByUserId(String userId);

}