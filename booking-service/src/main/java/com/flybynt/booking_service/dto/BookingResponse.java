package com.flybynt.booking_service.dto;

import com.flybynt.booking_service.model.*;
import java.math.BigDecimal;
import java.time.Instant;
public record BookingResponse(String id,String bookingReference,String passengerId,String flightId,
                              String seatId,BigDecimal totalPrice,BookingStatus status,Instant createdAt) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.id,b.bookingReference,b.passengerId,b.flightId,b.seatId,b.totalPrice,b.status,b.createdAt); }
}
