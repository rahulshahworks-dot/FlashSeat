package com.rahul.flashseat.config;

import com.rahul.flashseat.model.Seat;
import com.rahul.flashseat.model.SeatStatus;
import com.rahul.flashseat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SeatRepository seatRepository;

    @Override
    public void run(String... args) {
        Long showId = 1L;
        if (seatRepository.findByShowId(showId).isEmpty()) {
            List<String> validSeatNumbers = List.of(
                    "A1", "A2", "A3", "A4", "A5",
                    "B1", "B2", "B3", "B4", "B5"
            );

            List<Seat> seats = validSeatNumbers.stream()
                    .map(seatNo -> Seat.builder()
                            .showId(showId)
                            .seatNumber(seatNo)
                            .status(SeatStatus.AVAILABLE)
                            .build())
                    .toList();

            seatRepository.saveAll(seats);
            System.out.println(">>> SEEDED 10 VALID SEATS FOR SHOW 1 <<<");
        }
    }
}