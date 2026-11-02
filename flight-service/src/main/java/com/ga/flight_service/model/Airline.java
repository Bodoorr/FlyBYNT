package com.ga.flight_service.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Airline {
    private String iataCode;
    private String name;
}
