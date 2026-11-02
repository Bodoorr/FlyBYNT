package com.flybynt.booking_service.service;

import com.flybynt.booking_service.client.*;
import com.flybynt.booking_service.concurrency.PassengerLockManager;
import com.flybynt.booking_service.dto.*;
import com.flybynt.booking_service.exception.ApiException;
import com.flybynt.booking_service.model.*;
import com.flybynt.booking_service.repository.BookingRepository;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

/** Booking orchestration. Seat ownership is authoritative in Seat Service. */
@Service
public class BookingService {
    private final BookingRepository repository;
    private final FlightClient flights;
    private final PassengerClient passengers;
    private final SeatClient seats;
    private final PassengerLockManager locks;
    public BookingService(BookingRepository repository,FlightClient flights,PassengerClient passengers,SeatClient seats,PassengerLockManager locks){
        this.repository=repository;
        this.flights=flights;
        this.passengers=passengers;
        this.seats=seats;
        this.locks=locks;
    }
    /**
     * Creates one booking. Same-passenger checks are serialized within this JVM only.
     * External-service timeouts are ambiguous: keep PENDING and reconcile, never blindly release.
     */
    public BookingResponse create(CreateBookingRequest req,String userId,String token){
        ReentrantLock lock=locks.forPassenger(req.passengerId());lock.lock();
        try {
            Optional<Booking> prior=repository.findByUserIdAndIdempotencyKey(userId,req.idempotencyKey());

            if(prior.isPresent()){
                Booking b=prior.get();

                if(!b.flightId.equals(req.flightId())||!b.passengerId.equals(req.passengerId())||!b.seatId.equals(req.seatId()))
                    throw new ApiException(HttpStatus.CONFLICT,"Idempotency key reused for a different request");

                return BookingResponse.from(b);
            }
            passengers.verify(req.passengerId(),userId,token);
            FlightResponse flight=flights.get(req.flightId());
            if(flight.departureTime()==null||flight.arrivalTime()==null||flight.price()==null||
                    !flight.departureTime().isBefore(flight.arrivalTime()) || !flight.departureTime().isAfter(Instant.now()))
                throw new ApiException(HttpStatus.BAD_REQUEST,"Invalid or departed flight");

            for(Booking existing:repository.findByPassengerIdAndActiveTrue(req.passengerId())){

                if(existing.flightId.equals(req.flightId()))throw new ApiException(HttpStatus.CONFLICT,"Passenger already booked on this flight");

                if(existing.departureTime.isBefore(flight.arrivalTime()) && existing.arrivalTime.isAfter(flight.departureTime()))

                    throw new ApiException(HttpStatus.CONFLICT,"Passenger has overlapping flight");
            }
            Booking b=new Booking();

            b.bookingReference="TS-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
            b.idempotencyKey=req.idempotencyKey();b.userId=userId;b.passengerId=req.passengerId();
            b.flightId=req.flightId();b.seatId=req.seatId();b.departureTime=flight.departureTime();
            b.arrivalTime=flight.arrivalTime();b.totalPrice=flight.price();b.status=BookingStatus.PENDING;
            b.active=true;b.createdAt=Instant.now();b.updatedAt=b.createdAt;

            try {
                b=repository.save(b);
            }

            catch(DuplicateKeyException ex){
                return repository.findByUserIdAndIdempotencyKey(userId,req.idempotencyKey())
                        .filter(x->x.flightId.equals(req.flightId())&&x.passengerId.equals(req.passengerId())&&x.seatId.equals(req.seatId()))
                        .map(BookingResponse::from).orElseThrow(()->new ApiException(HttpStatus.CONFLICT,"Passenger already has an active booking or request key reused"));
            }
            // Hold failure is definitive only for an explicit 409. Network failures are ambiguous.
            String holdId;
            try {holdId=seats.hold(b.flightId,b.seatId,b.id,token);}
            catch(ApiException ex){
                if(ex.status==HttpStatus.CONFLICT){markFailed(b);}
                throw ex;
            }
            catch(HttpClientErrorException.Conflict ex){markFailed(b);throw new ApiException(HttpStatus.CONFLICT,"Seat unavailable");}
            b.holdId=holdId;b.updatedAt=Instant.now();repository.save(b);
            // Confirm may succeed remotely even if response is lost. Preserve PENDING on errors.
            seats.confirm(holdId,token);
            b.status=BookingStatus.CONFIRMED;b.updatedAt=Instant.now();repository.save(b);
            return BookingResponse.from(b);

        }
        finally {lock.unlock();}
    }
    private void markFailed(Booking b){
        b.status=BookingStatus.FAILED;
        b.active=false;
        b.updatedAt=Instant.now();
        repository.save(b);
    }

    /** Each passenger succeeds or fails independently; partial success is explicit. */
    public List<FamilyBookingResult> createFamily(FamilyBookingRequest request,String userId,String token){
        List<FamilyBookingResult> results=new ArrayList<>();
        for(CreateBookingRequest item:request.bookings()){
            try {
                results.add(new FamilyBookingResult(item.passengerId(),item.seatId(),create(item,userId,token),null));
            }

            catch(Exception ex){results.add(new FamilyBookingResult(item.passengerId(),item.seatId(),null,
                    ex instanceof ApiException?ex.getMessage():"Uncertain outcome; check booking status before retry"));}
        }
        return results;
    }

    public BookingResponse get(String id,String userId){
        Booking b=repository.findById(id)
                .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Booking not found"));

        if(!b.userId.equals(userId))throw new ApiException(HttpStatus.FORBIDDEN,"Not your booking");
        return BookingResponse.from(b);
    }

    public List<BookingResponse> mine(String userId){
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(BookingResponse::from).toList();
    }

    /** Cancellation only marks local state after Seat Service confirms release. */
    public BookingResponse cancel(String id,String userId,String token){
        Booking b=repository.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Booking not found"));
        if(!b.userId.equals(userId))throw new ApiException(HttpStatus.FORBIDDEN,"Not your booking");
        ReentrantLock lock=locks.forPassenger(b.passengerId);lock.lock();

        try{

            b=repository.findById(id).orElseThrow();
            if(b.status==BookingStatus.CANCELLED)return BookingResponse.from(b);
            if(b.status!=BookingStatus.CONFIRMED)throw new ApiException(HttpStatus.CONFLICT,"Only confirmed bookings can be cancelled");
            seats.cancel(b.id,token); // TODO(team): verify exact cancel endpoint and authorization
            b.status=BookingStatus.CANCELLED;b.active=false;b.updatedAt=Instant.now();
            return BookingResponse.from(repository.save(b));
        }

        finally{
            lock.unlock();
        }
    }
}
