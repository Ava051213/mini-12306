package com.mia.mini12306.train.dto;

import com.mia.mini12306.train.TrainTripIntervalPrice;

import java.math.BigDecimal;

public record SeatClassResponse(
        Integer type,
        BigDecimal price
) {
    // Cents to yuan with scale 2; exact, no rounding.
    public static SeatClassResponse from(TrainTripIntervalPrice row) {
        return new SeatClassResponse(
                row.getSeatType(),
                BigDecimal.valueOf(row.getPriceCents(), 2)
        );
    }
}
