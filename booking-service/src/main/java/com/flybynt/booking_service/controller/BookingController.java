package com.flybynt.booking_service.controller;

import com.flybynt.booking_service.dto.*;
import com.flybynt.booking_service.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** User-facing booking endpoints. No admin endpoints exist. */
@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    /** Creates a booking for an authorized passenger. */
    @PostMapping
    public ResponseEntity<BookingResponse> create(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(request, jwt.getSubject(), jwt.getTokenValue()));
    }

    /** Creates separate bookings for multiple passengers. */
    @PostMapping("/family")
    public List<FamilyBookingResult> family(
            @Valid @RequestBody FamilyBookingRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return service.createFamily(request, jwt.getSubject(), jwt.getTokenValue());
    }

    /** Lists only the authenticated user's bookings. */
    @GetMapping("/me")
    public List<BookingResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(jwt.getSubject());
    }

    /** Returns a booking only when it belongs to the authenticated user. */
    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return service.get(id, jwt.getSubject());
    }

    /** Cancels a booking only when it belongs to the authenticated user. */
    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return service.cancel(id, jwt.getSubject(), jwt.getTokenValue());
    }
}
