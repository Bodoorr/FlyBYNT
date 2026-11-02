package com.flybynt.booking_service.dto;

/** Subset of Passenger Service's GET /passengers/{id} response; other fields are ignored. */
public record PassengerResponse(String id) {}
