package com.ga.seat_service.controller;

import com.ga.seat_service.models.dtos.response.SeatResponse;
import com.ga.seat_service.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class SeatController {
    private final SeatService seatService;

    @GetMapping("/flights/{flightId}/seats")
    public ResponseEntity<List<SeatResponse>> getSeats(@PathVariable String flightId,
                                                       @RequestParam String aircraftId) {
        return ResponseEntity.ok(seatService.getSeatsForFlight(flightId, aircraftId));
    }

    @GetMapping("/flights/{flightId}/seats/available")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats(@PathVariable String flightId) {
        return ResponseEntity.ok(seatService.getAvailableSeats(flightId));
    }

    @GetMapping("/flights/{flightId}/seats/{seatId}")
    public ResponseEntity<SeatResponse> getSeat(@PathVariable String flightId,
                                                @PathVariable String seatId) {
        return ResponseEntity.ok(seatService.getSeat(flightId, seatId));
    }
}
