package com.flybynt.booking_service.service;

import com.flybynt.booking_service.model.Booking;
import com.flybynt.booking_service.model.BookingStatus;
import com.flybynt.booking_service.repository.BookingRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Resolves stuck PENDING bookings so a passenger is never blocked indefinitely. */
@Component
public class BookingReconciler {
    private static final Logger log=LoggerFactory.getLogger(BookingReconciler.class);
    // Seat holds last 10 minutes; past this age no hold for the booking can still be live.
    private static final Duration STALE_AFTER=Duration.ofMinutes(11);

    private final BookingRepository repository;
    private final BookingService service;
    public BookingReconciler(BookingRepository repository,BookingService service){
        this.repository=repository;
        this.service=service;
    }

    @Scheduled(fixedDelay=60_000)
    public void reconcilePending(){
        for(Booking b:repository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING,Instant.now().minus(STALE_AFTER))){
            try {service.reconcile(b);}
            catch(RuntimeException ex){log.warn("Could not reconcile booking {}: {}",b.id,ex.getMessage());}
        }
    }
}
