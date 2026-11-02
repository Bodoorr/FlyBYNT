package com.flybynt.booking_service.model;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/** One passenger + one flight + one seat. No raw CPR stored here. */
@Document("bookings")
@CompoundIndexes({
        @CompoundIndex(name="unique_request",def="{'userId':1,'idempotencyKey':1}",unique=true),
        @CompoundIndex(name="unique_active_passenger_flight",def="{'passengerId':1,'flightId':1}",unique=true,partialFilter="{'active':true}")
})
public class Booking {
    @Id public String id;
    public String bookingReference, idempotencyKey, userId, passengerId, flightId, seatId, holdId;
    public Instant departureTime, arrivalTime, createdAt, updatedAt;
    public BigDecimal totalPrice;
    public BookingStatus status;
    public boolean active;
    public Booking() {}
}
