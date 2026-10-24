package com.mia.mini12306.train.dto;

import com.mia.mini12306.train.TrainTrip;

import java.time.LocalDateTime;

public record TrainTripResponse(
        Long id,
        String trainNumber,
        String departureStation,
        String arrivalStation,
        LocalDateTime departureAt,
        LocalDateTime arrivalAt
) {
    // Keep the API response separate from the database entity.
    public static TrainTripResponse from(TrainTrip trip) {
        return new TrainTripResponse(
                trip.getId(),
                trip.getTrainNumber(),
                trip.getDepartureStation(),
                trip.getArrivalStation(),
                trip.getDepartureAt(),
                trip.getArrivalAt()
        );
    }
}