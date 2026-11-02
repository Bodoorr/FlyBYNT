package com.ga.seat_service.models;

import com.ga.seat_service.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "seat_inventory")
@CompoundIndex(name = "flight_seat_unique", def = "{'flightId': 1, 'seatId': 1}", unique = true)

public class SeatInventory {

        @Id
        private String id;

        private String flightId;
        private String seatId;

        @Builder.Default
        private SeatStatus status = SeatStatus.AVAILABLE;

        private String bookingId;
        private String holdToken;
        private Instant holdExpiresAt;

        @Version
        private Long version;
    }
