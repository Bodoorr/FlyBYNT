package com.flybynt.booking_service.dto;

/** TODO(team): confirm exact Seat Service endpoint and field names. */
public record SeatHoldRequest(String flightId,String seatId,String bookingId) {}
