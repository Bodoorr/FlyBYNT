package com.ga.seat_service.controller;

import com.ga.seat_service.models.dtos.request.ConfirmSeatRequest;
import com.ga.seat_service.models.dtos.request.ReleaseSeatRequest;
import com.ga.seat_service.models.dtos.response.HoldResponse;
import com.ga.seat_service.models.dtos.response.SeatResponse;
import com.ga.seat_service.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/flights/{flightId}/seats/{seatId}/hold")
    public ResponseEntity<HoldResponse> holdSeat(@PathVariable String flightId,
                                                 @PathVariable String seatId) {
        return ResponseEntity.ok(seatService.holdSeat(flightId, seatId));
    }

    @PostMapping("/flights/{flightId}/seats/{seatId}/confirm")
    public ResponseEntity<SeatResponse> confirmSeat(@PathVariable String flightId,
                                                    @PathVariable String seatId,
                                                    @Valid @RequestBody ConfirmSeatRequest request) {
        return ResponseEntity.ok(seatService.confirmSeat(flightId, seatId,
                request.getHoldToken(), request.getBookingId()));
    }

    @PostMapping("/flights/{flightId}/seats/{seatId}/release")
    public ResponseEntity<SeatResponse> releaseSeat(@PathVariable String flightId,
                                                    @PathVariable String seatId,
                                                    @Valid @RequestBody ReleaseSeatRequest request) {
        return ResponseEntity.ok(seatService.releaseSeat(flightId, seatId, request.getHoldToken()));
    }

    @PostMapping("/seats/cancel/{bookingId}")
    public ResponseEntity<List<SeatResponse>> cancelBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(seatService.cancelBooking(bookingId));
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
