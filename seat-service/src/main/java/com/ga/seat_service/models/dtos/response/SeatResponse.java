package com.ga.seat_service.models.dtos.response;

import com.ga.seat_service.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private String seatId;
    private String seatNumber;
    private SeatStatus seatStatus;
}
