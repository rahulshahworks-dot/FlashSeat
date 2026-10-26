package com.rahul.flashseat.service;

import com.rahul.flashseat.dto.SeatLockRequest;
import com.rahul.flashseat.dto.SeatLockResponse;
import com.rahul.flashseat.model.Booking;
import com.rahul.flashseat.model.Seat;
import com.rahul.flashseat.model.SeatStatus;
import com.rahul.flashseat.repository.BookingRepository;
import com.rahul.flashseat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatReservationService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> seatLockScript;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;

    private static final String ADMISSION_TOKEN_PREFIX = "gate_token:";
    private static final int LOCK_TTL_SECONDS = 300; // 5 minutes

    @Transactional
    public SeatLockResponse lockAndReserveSeats(SeatLockRequest request) {
        // 1. Gatekeeper Token Validation
        String storedToken = redisTemplate.opsForValue().get(ADMISSION_TOKEN_PREFIX + request.getUserId());
        if (storedToken == null || !storedToken.equals(request.getGateToken())) {
            return SeatLockResponse.builder()
                    .success(false)
                    .message("Unauthorized access. Token expired or invalid. Please wait in queue.")
                    .build();
        }

        // 2. Defensive Validation: Check if requested seats physically exist in DB
        List<Seat> existingSeats = seatRepository.findByShowId(request.getShowId()).stream()
                .filter(s -> request.getSeatNumbers().contains(s.getSeatNumber()))
                .toList();

        // Agar user ne koi fake seat (jaise Z999) bhej di
        if (existingSeats.size() != request.getSeatNumbers().size()) {
            return SeatLockResponse.builder()
                    .success(false)
                    .message("One or more seat numbers are invalid or do not exist for this show.")
                    .build();
        }

        // Agar koi seat pehle se permanently BOOKED hai
        boolean hasAlreadyBookedSeat = existingSeats.stream()
                .anyMatch(s -> s.getStatus() == SeatStatus.BOOKED);

        if (hasAlreadyBookedSeat) {
            return SeatLockResponse.builder()
                    .success(false)
                    .message("One or more seats are already booked.")
                    .build();
        }

        // 3. Redis Key Formatting: show:<id>:seat:<seatNumber>
        List<String> redisKeys = request.getSeatNumbers().stream()
                .map(seatNo -> "show:" + request.getShowId() + ":seat:" + seatNo)
                .toList();

        // 4. Execute Lua Script atomically (Check & Set in-memory locks)
        Long result = redisTemplate.execute(
                seatLockScript,
                redisKeys,
                request.getUserId(),
                String.valueOf(LOCK_TTL_SECONDS)
        );

        if (result == null || result == 0L) {
            return SeatLockResponse.builder()
                    .success(false)
                    .message("One or more seats are currently held by another user.")
                    .build();
        }

        // 5. Persistence to PostgreSQL
        String bookingRef = "BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        for (Seat seat : existingSeats) {
            seat.setStatus(SeatStatus.BOOKED);
            seatRepository.save(seat);
        }

        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .showId(request.getShowId())
                .seatNumbers(request.getSeatNumbers())
                .bookingReference(bookingRef)
                .bookingTime(LocalDateTime.now())
                .build();

        bookingRepository.save(booking);

        // 6. Invalidate gate token taaki single-use rahe
        redisTemplate.delete(ADMISSION_TOKEN_PREFIX + request.getUserId());

        return SeatLockResponse.builder()
                .success(true)
                .message("Seats successfully reserved!")
                .bookingReference(bookingRef)
                .lockedSeats(request.getSeatNumbers())
                .build();
    }

    // 7. Lightweight Layout View: Live status merge
    public Map<String, SeatStatus> getShowSeatLayout(Long showId) {
        List<Seat> seats = seatRepository.findByShowId(showId);

        return seats.stream().collect(Collectors.toMap(
                Seat::getSeatNumber,
                seat -> {
                    if (seat.getStatus() == SeatStatus.BOOKED) {
                        return SeatStatus.BOOKED;
                    }
                    Boolean isLocked = redisTemplate.hasKey("show:" + showId + ":seat:" + seat.getSeatNumber());
                    return Boolean.TRUE.equals(isLocked) ? SeatStatus.LOCKED : SeatStatus.AVAILABLE;
                }
        ));
    }
}