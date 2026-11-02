package com.ga.seat_service.service;

import com.ga.seat_service.enums.SeatPosition;
import com.ga.seat_service.enums.SeatStatus;
import com.ga.seat_service.exceptions.InvalidSeatLayoutException;
import com.ga.seat_service.exceptions.SeatNotFoundException;
import com.ga.seat_service.exceptions.SeatUnAvailableException;
import com.ga.seat_service.exceptions.SeatsAlreadyExistException;
import com.ga.seat_service.models.Seat;
import com.ga.seat_service.models.SeatInventory;
import com.ga.seat_service.models.dtos.request.GenerateSeatsRequest;
import com.ga.seat_service.models.dtos.request.SeatSectionRequest;
import com.ga.seat_service.models.dtos.response.BookingHoldResponse;
import com.ga.seat_service.models.dtos.response.HoldResponse;
import com.ga.seat_service.models.dtos.response.SeatResponse;
import com.ga.seat_service.repository.SeatInventoryRepository;
import com.ga.seat_service.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class SeatService {
    private final SeatRepository seatRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);

    // builds every seat for an aircraft from its sections, e.g. rows 1-3 "ACDF" -> 1A,1C,1D,1F,2A...
    public List<Seat> generateSeats(String aircraftId, GenerateSeatsRequest request) {
        if (seatRepository.existsByAircraftId(aircraftId)) {
            throw new SeatsAlreadyExistException("Aircraft " + aircraftId + " already has seats");
        }

        List<Seat> seats = new ArrayList<>();
        Set<String> seatNumbers = new HashSet<>();

        for (SeatSectionRequest section : request.getSections()) {
            if (section.getFromRow() > section.getToRow()) {
                throw new InvalidSeatLayoutException("fromRow " + section.getFromRow()
                        + " is after toRow " + section.getToRow());
            }

            String letters = section.getLetters().toUpperCase();
            String windows = section.getWindowLetters().toUpperCase();
            String aisles = section.getAisleLetters().toUpperCase();

            for (int row = section.getFromRow(); row <= section.getToRow(); row++) {
                for (char letter : letters.toCharArray()) {
                    String seatNumber = row + String.valueOf(letter);

                    // catches overlapping row ranges or repeated letters like "AAB"
                    if (!seatNumbers.add(seatNumber)) {
                        throw new InvalidSeatLayoutException("Seat " + seatNumber + " appears more than once");
                    }

                    seats.add(Seat.builder()
                            .aircraftId(aircraftId)
                            .seatNumber(seatNumber)
                            .rowNumber(row)
                            .seatClass(section.getSeatClass())
                            .position(positionOf(letter, windows, aisles))
                            .build());
                }
            }
        }

        return seatRepository.saveAll(seats);
    }

    private SeatPosition positionOf(char letter, String windowLetters, String aisleLetters) {
        if (windowLetters.indexOf(letter) >= 0) {
            return SeatPosition.WINDOW;
        }
        if (aisleLetters.indexOf(letter) >= 0) {
            return SeatPosition.AISLE;
        }
        return SeatPosition.MIDDLE;
    }

    public List<SeatResponse> getSeatsForFlight(String flightId , String aircraftId){
        List<Seat> seats = seatRepository.findByAircraftIdOrderByRowNumberAscSeatNumberAsc(aircraftId);
        if (seats.isEmpty()) {
            throw new SeatNotFoundException("No seats for aircraft " + aircraftId);
        }
        // booking holds can create single rows, so fill in only the seats that have none yet
        Map<String, SeatInventory> inventory = seatInventoryRepository.findByFlightId(flightId)
                .stream()
                .collect(Collectors.toMap(SeatInventory::getSeatId, inv -> inv));
        for (Seat seat : seats) {
            if (!inventory.containsKey(seat.getId())) {
                inventory.put(seat.getId(), findOrCreateInventory(flightId, seat.getId()));
            }
        }

        Map<String, SeatStatus> statusMap = inventory.values().stream()
                .collect(Collectors.toMap(SeatInventory::getSeatId, this::displayStatus));

        return seats.stream()
                .map(s -> new SeatResponse(s.getId(), s.getSeatNumber(), statusMap.get(s.getId())))
                .toList();
    }

    public HoldResponse holdSeat(String flightId, String seatId) {
        SeatInventory inv = findInventory(flightId, seatId);

        if (!isFree(inv)) {
            throw new SeatUnAvailableException("Seat " + seatId + " is not available");
        }

        inv.setStatus(SeatStatus.HELD);
        inv.setHoldToken(UUID.randomUUID().toString());
        inv.setHoldExpiresAt(Instant.now().plus(HOLD_DURATION));
        seatInventoryRepository.save(inv);

        return new HoldResponse(flightId, seatId, inv.getHoldToken(), inv.getHoldExpiresAt());
    }

    public SeatResponse confirmSeat(String flightId, String seatId, String holdToken, String bookingId) {
        SeatInventory inv = findInventory(flightId, seatId);

        if (inv.getStatus() != SeatStatus.HELD) {
            throw new SeatUnAvailableException("Seat " + seatId + " is not held");
        }
        if (!holdToken.equals(inv.getHoldToken())) {
            throw new SeatUnAvailableException("Invalid hold token for seat " + seatId);
        }
        if (isExpired(inv)) {
            throw new SeatUnAvailableException("Hold on seat " + seatId + " has expired");
        }

        inv.setStatus(SeatStatus.BOOKED);
        inv.setBookingId(bookingId);
        inv.setHoldToken(null);
        inv.setHoldExpiresAt(null);

        return toResponse(seatInventoryRepository.save(inv));
    }

    public SeatResponse releaseSeat(String flightId, String seatId, String holdToken) {
        SeatInventory inv = findInventory(flightId, seatId);

        if (inv.getStatus() != SeatStatus.HELD) {
            throw new SeatUnAvailableException("Seat " + seatId + " is not held");
        }
        if (!holdToken.equals(inv.getHoldToken())) {
            throw new SeatUnAvailableException("Invalid hold token for seat " + seatId);
        }

        inv.setStatus(SeatStatus.AVAILABLE);
        inv.setHoldToken(null);
        inv.setHoldExpiresAt(null);

        return toResponse(seatInventoryRepository.save(inv));
    }

    // a booking can have several seats, so free all of them
    public List<SeatResponse> cancelBooking(String bookingId) {
        List<SeatInventory> booked = seatInventoryRepository.findByBookingIdAndStatus(bookingId, SeatStatus.BOOKED);
        if (booked.isEmpty()) {
            throw new SeatNotFoundException("No booked seats for booking " + bookingId);
        }

        for (SeatInventory inv : booked) {
            inv.setStatus(SeatStatus.AVAILABLE);
            inv.setBookingId(null);
        }

        return seatInventoryRepository.saveAll(booked).stream()
                .map(this::toResponse)
                .toList();
    }

    // ---- used by booking-service: the hold is identified by its holdToken ----

    public BookingHoldResponse holdForBooking(String flightId, String seatId, String bookingId) {
        SeatInventory inv = findOrCreateInventory(flightId, seatId);

        // retry of the same request: hand back the hold this booking already has
        if (inv.getStatus() == SeatStatus.HELD && !isExpired(inv) && bookingId.equals(inv.getBookingId())) {
            return new BookingHoldResponse(true, inv.getHoldToken(), inv.getHoldExpiresAt());
        }

        if (!isFree(inv)) {
            throw new SeatUnAvailableException("Seat " + seatId + " is not available");
        }

        inv.setStatus(SeatStatus.HELD);
        inv.setBookingId(bookingId);
        inv.setHoldToken(UUID.randomUUID().toString());
        inv.setHoldExpiresAt(Instant.now().plus(HOLD_DURATION));
        seatInventoryRepository.save(inv);

        return new BookingHoldResponse(true, inv.getHoldToken(), inv.getHoldExpiresAt());
    }

    // safe to call twice: confirming an already BOOKED hold just returns it
    public SeatResponse confirmHold(String holdToken) {
        SeatInventory inv = findByHoldToken(holdToken);

        if (inv.getStatus() == SeatStatus.BOOKED) {
            return toResponse(inv);
        }
        if (inv.getStatus() != SeatStatus.HELD || isExpired(inv)) {
            throw new SeatUnAvailableException("Hold " + holdToken + " has expired or is no longer valid");
        }

        // holdToken is kept on BOOKED seats so a retried confirm still finds it
        inv.setStatus(SeatStatus.BOOKED);
        inv.setHoldExpiresAt(null);
        return toResponse(seatInventoryRepository.save(inv));
    }

    public SeatResponse releaseHold(String holdToken) {
        SeatInventory inv = findByHoldToken(holdToken);

        if (inv.getStatus() == SeatStatus.BOOKED) {
            throw new SeatUnAvailableException("Seat is already booked, cancel the booking instead");
        }

        makeAvailable(inv);
        return toResponse(seatInventoryRepository.save(inv));
    }

    // safe to call twice: a booking with nothing left to cancel returns an empty list
    public List<SeatResponse> cancelBookingSeats(String bookingId) {
        List<SeatInventory> booked = seatInventoryRepository.findByBookingIdAndStatus(bookingId, SeatStatus.BOOKED);

        booked.forEach(this::makeAvailable);
        return seatInventoryRepository.saveAll(booked).stream()
                .map(this::toResponse)
                .toList();
    }

    public SeatResponse getSeat(String flightId, String seatId) {
        return toResponse(findInventory(flightId, seatId));
    }

    public List<SeatResponse> getAvailableSeats(String flightId) {
        Map<String, SeatInventory> available = seatInventoryRepository
                .findByFlightId(flightId)
                .stream()
                .filter(this::isFree)
                .collect(Collectors.toMap(SeatInventory::getSeatId, inv -> inv));

        return seatRepository.findAllById(available.keySet()).stream()
                .sorted(Comparator.comparingInt(Seat::getRowNumber).thenComparing(Seat::getSeatNumber))
                .map(s -> new SeatResponse(s.getId(), s.getSeatNumber(), displayStatus(available.get(s.getId()))))
                .toList();
    }



    private boolean isExpired(SeatInventory inv) {
        return inv.getHoldExpiresAt() != null && inv.getHoldExpiresAt().isBefore(Instant.now());
    }

    // a seat is free if it's AVAILABLE, or HELD but the hold time has run out
    private boolean isFree(SeatInventory inv) {
        return inv.getStatus() == SeatStatus.AVAILABLE
                || (inv.getStatus() == SeatStatus.HELD && isExpired(inv));
    }

    // what the user should see: an expired hold shows as AVAILABLE
    private SeatStatus displayStatus(SeatInventory inv) {
        return isFree(inv) ? SeatStatus.AVAILABLE : inv.getStatus();
    }

    private void makeAvailable(SeatInventory inv) {
        inv.setStatus(SeatStatus.AVAILABLE);
        inv.setBookingId(null);
        inv.setHoldToken(null);
        inv.setHoldExpiresAt(null);
    }

    private SeatInventory findByHoldToken(String holdToken) {
        return seatInventoryRepository.findByHoldToken(holdToken)
                .orElseThrow(() -> new SeatNotFoundException("Hold " + holdToken + " not found"));
    }

    // booking-service never opens the seat map, so the flight's inventory row may not exist yet
    private SeatInventory findOrCreateInventory(String flightId, String seatId) {
        Optional<SeatInventory> existing = seatInventoryRepository.findByFlightIdAndSeatId(flightId, seatId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException("Seat " + seatId + " not found"));

        SeatInventory inv = new SeatInventory();
        inv.setFlightId(flightId);
        inv.setSeatId(seat.getId());
        inv.setStatus(SeatStatus.AVAILABLE);
        try {
            return seatInventoryRepository.save(inv);
        } catch (DuplicateKeyException ex) {
            // another request created it at the same moment (unique flightId+seatId index)
            return findInventory(flightId, seatId);
        }
    }

    private SeatInventory findInventory(String flightId, String seatId) {
        return seatInventoryRepository.findByFlightIdAndSeatId(flightId, seatId)
                .orElseThrow(() -> new SeatNotFoundException("Seat " + seatId + " not found on flight " + flightId));
    }

    private SeatResponse toResponse(SeatInventory inv) {
        Seat seat = seatRepository.findById(inv.getSeatId())
                .orElseThrow(() -> new SeatNotFoundException("Seat not found"));
        return new SeatResponse(seat.getId(), seat.getSeatNumber(), displayStatus(inv));
    }

}
