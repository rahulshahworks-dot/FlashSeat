package com.rahul.flashseat.controller;

import com.rahul.flashseat.dto.SeatLockRequest;
import com.rahul.flashseat.dto.SeatLockResponse;
import com.rahul.flashseat.model.SeatStatus;
import com.rahul.flashseat.service.SeatReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatReservationService seatReservationService;

    @GetMapping("/layout")
    public ResponseEntity<Map<String, SeatStatus>> getSeatLayout(@RequestParam Long showId) {
        return ResponseEntity.ok(seatReservationService.getShowSeatLayout(showId));
    }

    @PostMapping("/reserve")
    public ResponseEntity<SeatLockResponse> reserveSeats(@RequestBody SeatLockRequest request) {
        SeatLockResponse response = seatReservationService.lockAndReserveSeats(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}