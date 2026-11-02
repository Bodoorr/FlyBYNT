package com.ga.passenger_Service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.Instant;

@Document(collection = "passengers")
public class Passenger {

    @Id
    private String id;

    @NotBlank(message = "User ID is required")
    @Indexed(unique = true)
    private String userId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Nationality is required")
    private String nationality;

    @NotBlank(message = "Passport number is required")
    @Indexed(unique = true)
    private String passportNumber;

    @NotNull(message = "Creation date is required")
    private Instant createdAt;
}
