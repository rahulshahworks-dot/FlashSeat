package com.rahul.flashseat.repository;

import com.rahul.flashseat.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    Optional<Seat> findByShowIdAndSeatNumber(Long showId, String seatNumber);
    List<Seat> findByShowId(Long showId);
}