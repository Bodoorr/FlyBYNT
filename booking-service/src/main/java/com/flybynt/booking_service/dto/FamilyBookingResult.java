package com.flybynt.booking_service.dto;

public record FamilyBookingResult(String passengerId,String seatId,BookingResponse booking,String error) {}
