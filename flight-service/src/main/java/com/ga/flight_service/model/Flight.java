package com.ga.flight_service.model;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "flights")
@ToString
public class Flight {
    @Id
    private String id;

    private String flightNumber;
    private Airport departure;
    private Airport arrival;
    private Airline airline;
    private Aircraft aircraft;
    private String status;

}

