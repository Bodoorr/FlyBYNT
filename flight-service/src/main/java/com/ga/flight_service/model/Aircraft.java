package com.ga.flight_service.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Aircraft {
    private String icaoCode;
    private String model;
    private Integer totalSeats;
    private Integer totalRows;
    private String seatLayout;
}

