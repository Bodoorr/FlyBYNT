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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/** Booking orchestration. Seat ownership is authoritative in Seat Service. */
@Service
public class BookingService {
    private final BookingRepository repository;
    private final FlightClient flights;
    private final PassengerClient passengers;
    private final SeatClient seats;
    private final PassengerLockManager locks;
    private final MongoTemplate mongo;
    public BookingService(BookingRepository repository,FlightClient flights,PassengerClient passengers,SeatClient seats,PassengerLockManager locks,MongoTemplate mongo){
        this.repository=repository;
        this.mongo=mongo;
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
            // Hold: any failure is FAILED. A hold we never heard about expires on its own after 10 minutes.
            String holdToken;
            try {holdToken=seats.hold(b.flightId,b.seatId,b.id,token);}
            catch(ApiException ex){resolvePending(b,BookingStatus.FAILED);throw ex;}
            catch(HttpClientErrorException ex){
                resolvePending(b,BookingStatus.FAILED);
                if(ex.getStatusCode().value()==404)throw new ApiException(HttpStatus.NOT_FOUND,"Seat not found");
                if(ex.getStatusCode().value()==409)throw new ApiException(HttpStatus.CONFLICT,"Seat unavailable");
                throw new ApiException(HttpStatus.BAD_REQUEST,"Seat hold rejected");
            }
            catch(RestClientException ex){
                resolvePending(b,BookingStatus.FAILED);
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Seat Service unavailable; booking failed, please retry");
            }
            mongo.updateFirst(byId(b.id),Update.update("holdToken",holdToken).set("updatedAt",Instant.now()),Booking.class);
            b.holdToken=holdToken;

            // Confirm: a 4xx is definitive; a timeout/5xx is ambiguous because the confirm may have landed.
            try {seats.confirm(holdToken,token);}
            catch(HttpClientErrorException ex){
                releaseQuietly(holdToken,token);
                resolvePending(b,BookingStatus.FAILED);
                throw new ApiException(HttpStatus.CONFLICT,"Seat hold expired or invalid");
            }
            catch(RestClientException ex){
                return BookingResponse.from(resolveFromSeatStatus(b,token));
            }
            return BookingResponse.from(resolvePending(b,BookingStatus.CONFIRMED));

        }
        finally {lock.unlock();}
    }

    /** After an ambiguous confirm: ask Seat Service. Only our hold token could have booked the seat seconds ago. */
    private Booking resolveFromSeatStatus(Booking b,String token){
        String seatStatus;
        try {seatStatus=seats.status(b.flightId,b.seatId,token);}
        catch(RestClientException ex){return b;} // still unknown: the reconciler resolves it
        if("BOOKED".equals(seatStatus))return resolvePending(b,BookingStatus.CONFIRMED);
        if("AVAILABLE".equals(seatStatus))return resolvePending(b,BookingStatus.FAILED);
        return b; // HELD: the reconciler resolves it once the hold has expired
    }

    /**
     * Resolves a PENDING booking left by a crash or an ambiguous Seat Service call. Called by the reconciler
     * only after every hold has expired, so re-confirming with our hold token is a safe probe:
     * it succeeds only if our hold is the one that booked the seat.
     */
    public void reconcile(Booking b){
        if(b.holdToken==null){resolvePending(b,BookingStatus.FAILED);return;}
        try {seats.confirm(b.holdToken,null);resolvePending(b,BookingStatus.CONFIRMED);}
        catch(HttpClientErrorException ex){resolvePending(b,BookingStatus.FAILED);}
        // timeout or 5xx: leave PENDING and try again on the next run
    }

    /** PENDING -> target, only if nothing else resolved it first. Returns the booking's current state. */
    private Booking resolvePending(Booking b,BookingStatus target){
        Query pending=byId(b.id).addCriteria(Criteria.where("status").is(BookingStatus.PENDING));
        Update update=new Update().set("status",target).set("active",target==BookingStatus.CONFIRMED).set("updatedAt",Instant.now());
        Booking updated=mongo.findAndModify(pending,update,FindAndModifyOptions.options().returnNew(true),Booking.class);
        return updated!=null?updated:repository.findById(b.id).orElseThrow();
    }

    private void releaseQuietly(String holdToken,String token){
        try {seats.release(holdToken,token);}
        catch(RestClientException ignored){} // best effort: the hold expires on its own
    }

    private static Query byId(String id){
        return Query.query(Criteria.where("_id").is(id));
    }

    /** Each passenger succeeds or fails independently; partial success is explicit. */
    public List<FamilyBookingResult> createFamily(FamilyBookingRequest request,String userId,String token){
        // reject duplicates before booking anything
        Set<String> passengerIds=new HashSet<>(),seatsOnFlight=new HashSet<>(),keys=new HashSet<>();
        for(CreateBookingRequest item:request.bookings()){
            if(!passengerIds.add(item.passengerId()))
                throw new ApiException(HttpStatus.BAD_REQUEST,"Passenger "+item.passengerId()+" appears more than once");
            if(!seatsOnFlight.add(item.flightId()+"/"+item.seatId()))
                throw new ApiException(HttpStatus.BAD_REQUEST,"Seat "+item.seatId()+" appears more than once");
            if(!keys.add(item.idempotencyKey()))
                throw new ApiException(HttpStatus.BAD_REQUEST,"Each passenger needs its own idempotency key");
        }
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
            seats.cancel(b.id,token);
            b.status=BookingStatus.CANCELLED;b.active=false;b.updatedAt=Instant.now();
            return BookingResponse.from(repository.save(b));
        }

        finally{
            lock.unlock();
        }
    }
}
