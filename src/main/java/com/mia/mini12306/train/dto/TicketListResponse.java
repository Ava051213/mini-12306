package com.mia.mini12306.train.dto;

import com.mia.mini12306.train.TrainTripInterval;

import java.time.LocalDateTime;

public record TicketListResponse(
        String trainId,
        String trainNumber,
        String departure,
        String arrival,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime
) {
    // Keep the reference field names even though the row uses other names.
    public static TicketListResponse from(TrainTripInterval interval) {
        return new TicketListResponse(
                String.valueOf(interval.getTrainTripId()),
                interval.getTrainNumber(),
                interval.getDepartureStation(),
                interval.getArrivalStation(),
                interval.getDepartureAt(),
                interval.getArrivalAt()
        );
    }
}
