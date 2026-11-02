package com.ga.seat_service.controller;

import com.ga.seat_service.models.dtos.request.BookingHoldRequest;
import com.ga.seat_service.models.dtos.response.BookingHoldResponse;
import com.ga.seat_service.models.dtos.response.SeatResponse;
import com.ga.seat_service.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// endpoints called by booking-service (see its SeatClient)
@RequiredArgsConstructor
@RestController
@RequestMapping("/seats")
public class BookingSeatController {
    private final SeatService seatService;

    @PostMapping("/hold")
    public ResponseEntity<BookingHoldResponse> hold(@Valid @RequestBody BookingHoldRequest request) {
        return ResponseEntity.ok(seatService.holdForBooking(
                request.getFlightId(), request.getSeatId(), request.getBookingId()));
    }

    @PostMapping("/holds/{holdToken}/confirm")
    public ResponseEntity<SeatResponse> confirm(@PathVariable String holdToken) {
        return ResponseEntity.ok(seatService.confirmHold(holdToken));
    }

    @PostMapping("/holds/{holdToken}/release")
    public ResponseEntity<SeatResponse> release(@PathVariable String holdToken) {
        return ResponseEntity.ok(seatService.releaseHold(holdToken));
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<List<SeatResponse>> cancel(@PathVariable String bookingId) {
        return ResponseEntity.ok(seatService.cancelBookingSeats(bookingId));
    }
}
