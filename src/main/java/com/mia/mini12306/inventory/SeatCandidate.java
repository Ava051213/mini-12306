package com.mia.mini12306.inventory;

// The seat chosen for one purchase, with its display numbers.
public record SeatCandidate(
        Long seatId,
        String carriageNumber,
        String seatNumber
) {

}
