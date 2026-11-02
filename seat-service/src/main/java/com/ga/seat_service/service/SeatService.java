package com.ga.seat_service.service;

import com.ga.seat_service.enums.SeatStatus;
import com.ga.seat_service.exceptions.SeatNotFoundException;
import com.ga.seat_service.models.Seat;
import com.ga.seat_service.models.SeatInventory;
import com.ga.seat_service.models.dtos.response.SeatResponse;
import com.ga.seat_service.repository.SeatInventoryRepository;
import com.ga.seat_service.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class SeatService {
    private final SeatRepository seatRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final MongoTemplate mongoTemplate;

    public List<SeatResponse> getSeatsForFlight(String flightId , String aircraftId){
        List<Seat> seats = seatRepository.findByAircraftIdOrderByRowNumberAscSeatNumberAsc(aircraftId);
        if (seats.isEmpty()) {
            throw new SeatNotFoundException("No seats for aircraft " + aircraftId);
        }
        if (!seatInventoryRepository.existsByFlightId(flightId)) {
            for (Seat seat : seats) {
                SeatInventory inv = new SeatInventory();
                inv.setFlightId(flightId);
                inv.setSeatId(seat.getId());
                inv.setStatus(SeatStatus.AVAILABLE);
                seatInventoryRepository.save(inv);
            }
        }
        Map<String, SeatStatus> statusMap = seatInventoryRepository.findByFlightId(flightId)
                .stream()
                .collect(Collectors.toMap(SeatInventory::getSeatId, SeatInventory::getStatus));

        return seats.stream()
                .map(s -> new SeatResponse(s.getId(), s.getSeatNumber(), statusMap.get(s.getId())))
                .toList();
    }


    public SeatResponse getSeat(String flightId, String seatId) {
        SeatInventory inv = seatInventoryRepository.findByFlightIdAndSeatId(flightId, seatId)
                .orElseThrow(() -> new SeatNotFoundException("Seat " + seatId + " not found on flight " + flightId));
        return toResponse(inv);
    }

    public List<SeatResponse> getAvailableSeats(String flightId) {
        Map<String, SeatInventory> available = seatInventoryRepository
                .findByFlightIdAndStatus(flightId, SeatStatus.AVAILABLE)
                .stream()
                .collect(Collectors.toMap(SeatInventory::getSeatId, inv -> inv));

        return seatRepository.findAllById(available.keySet()).stream()
                .sorted(Comparator.comparingInt(Seat::getRowNumber).thenComparing(Seat::getSeatNumber))
                .map(s -> new SeatResponse(s.getId(), s.getSeatNumber(), available.get(s.getId()).getStatus()))
                .toList();
    }


    private SeatResponse toResponse(SeatInventory inv) {
        Seat seat = seatRepository.findById(inv.getSeatId())
                .orElseThrow(() -> new SeatNotFoundException("Seat not found"));
        return new SeatResponse(seat.getId(), seat.getSeatNumber(), inv.getStatus());
    }

}
