package com.flybynt.booking_service.dto;

/** TODO(team): Passenger Service must verify ownership; do not trust a client CPR. */
public record PassengerResponse(String id,String userId,boolean authorized) {}
