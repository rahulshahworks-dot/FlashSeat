package com.rahul.flashseat.dto;

import com.rahul.flashseat.model.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeatStatusResponse {
    private String seatNumber;
    private SeatStatus status; // AVAILABLE, LOCKED, BOOKED
}