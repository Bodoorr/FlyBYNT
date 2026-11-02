package com.ga.seat_service.models.dtos.request;

import com.ga.seat_service.enums.SeatClass;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

// one block of rows with the same class and layout, e.g. rows 1-3 BUSINESS "ACDF"
@Data
public class SeatSectionRequest {
    @NotNull
    private SeatClass seatClass;

    @Positive
    @Max(100)
    private int fromRow;

    @Positive
    @Max(100)
    private int toRow;

    @NotNull
    @Pattern(regexp = "[A-Za-z]+", message = "must be letters only, e.g. ABCDEF")
    private String letters;

    @Pattern(regexp = "[A-Za-z]*", message = "must be letters only, e.g. AF")
    private String windowLetters = "";

    @Pattern(regexp = "[A-Za-z]*", message = "must be letters only, e.g. CD")
    private String aisleLetters = "";
}
