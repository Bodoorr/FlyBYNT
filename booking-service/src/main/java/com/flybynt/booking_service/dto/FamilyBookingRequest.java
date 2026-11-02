package com.flybynt.booking_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
/** Partial success is allowed; each passenger request has a distinct idempotency key. */
public record FamilyBookingRequest(
        @NotEmpty List<@Valid CreateBookingRequest> bookings) {

}
