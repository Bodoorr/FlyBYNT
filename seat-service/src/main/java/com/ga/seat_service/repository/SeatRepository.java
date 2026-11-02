package com.ga.seat_service.repository;

import com.ga.seat_service.models.Seat;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SeatRepository extends MongoRepository<Seat,String> {
    List<Seat> findByAircraftIdOrderByRowNumberAscSeatNumberAsc(String aircraftId);

}
