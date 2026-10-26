package com.rahul.flashseat.dto;

import lombok.Data;
import java.util.List;

@Data
public class SeatLockRequest {
    private String gateToken;
    private String userId;
    private Long showId;
    private List<String> seatNumbers; // ["A1", "A2"]
}