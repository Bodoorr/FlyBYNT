package com.ga.flight_service.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Airport {
    private String iataCode;
    private String name;
    private String city;
    private String country;
    private String countryCode;
}
