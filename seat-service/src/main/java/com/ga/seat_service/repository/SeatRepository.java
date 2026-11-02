package com.ga.seat_service.repository;

import com.ga.seat_service.models.SeatInventory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SeatRepository extends MongoRepository<SeatInventory,String> {

}
