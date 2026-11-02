package com.ga.seat_service.service;

import com.ga.seat_service.models.dtos.response.SeatResponse;
import com.ga.seat_service.repository.SeatInventoryRepository;
import com.ga.seat_service.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class SeatService {
    private final SeatRepository seatRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final MongoTemplate mongoTemplate;

    public List<SeatResponse> getSeatsForFlight(String flightId , String aircraftId){

    }

}
