package com.rahul.flashseat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeatLockResponse {
    private boolean success;
    private String message;
    private String bookingReference;
    private List<String> lockedSeats;
}