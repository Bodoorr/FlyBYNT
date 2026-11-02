package com.ga.seat_service.repository;

import com.ga.seat_service.enums.SeatStatus;
import com.ga.seat_service.models.SeatInventory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SeatInventoryRepository extends MongoRepository<SeatInventory, String> {
    List<SeatInventory> findByFlightId(String flightId);

    boolean existsByFlightId(String flightId);

    Optional<SeatInventory> findByFlightIdAndSeatId(String flightId, String seatId);

    List<SeatInventory> findByFlightIdAndStatus(String flightId, SeatStatus status);

    List<SeatInventory> findByBookingIdAndStatus(String bookingId, SeatStatus status);

    Optional<SeatInventory> findByHoldToken(String holdToken);
}
