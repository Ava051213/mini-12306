package com.mia.mini12306.train.dto;

import com.mia.mini12306.train.TrainTripInterval;

import java.time.LocalDateTime;
import java.util.List;

public record TicketListResponse(
        String trainId,
        String trainNumber,
        String departure,
        String arrival,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        List<SeatClassResponse> seatClassList
) {
    // An interval without configured prices stays in the list with an empty array.
    public static TicketListResponse from(
            TrainTripInterval interval,
            List<SeatClassResponse> seatClassList
    ) {
        return new TicketListResponse(
                String.valueOf(interval.getTrainTripId()),
                interval.getTrainNumber(),
                interval.getDepartureStation(),
                interval.getArrivalStation(),
                interval.getDepartureAt(),
                interval.getArrivalAt(),
                seatClassList
        );
    }
}
