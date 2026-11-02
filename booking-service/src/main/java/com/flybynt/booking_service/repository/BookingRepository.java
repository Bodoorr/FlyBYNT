package com.flybynt.booking_service.repository;

import com.flybynt.booking_service.model.Booking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface BookingRepository extends MongoRepository<Booking,String> {
    Optional<Booking> findByUserIdAndIdempotencyKey(String userId,String key);
    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Booking> findByPassengerIdAndActiveTrue(String passengerId);
    List<Booking> findByFlightIdAndActiveTrue(String flightId);
}
