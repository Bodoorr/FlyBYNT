package com.flybynt.booking_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
/** TODO(team): align response names and time semantics with Flight Service. */
public record FlightResponse(String id,Instant departureTime,Instant arrivalTime,BigDecimal price,String status) {}
