package com.ga.seat_service.models;

import com.ga.seat_service.enums.SeatClass;
import com.ga.seat_service.enums.SeatPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "seats")
@CompoundIndex(name = "aircraft_seat_unique", def = "{'aircraftId': 1, 'seatNumber': 1}", unique = true)

public class Seat {

    @Id
    private String id;

    private String aircraftId;

    private String seatNumber;

    private int rowNumber;

    private SeatClass seatClass;

    private SeatPosition position;

}
