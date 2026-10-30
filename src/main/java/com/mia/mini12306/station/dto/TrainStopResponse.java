package com.mia.mini12306.station.dto;

import com.mia.mini12306.station.TrainTripStopRow;

import java.time.Duration;
import java.time.LocalDateTime;

public record TrainStopResponse(
        String sequence,
        String departure,
        LocalDateTime arrivalTime,
        LocalDateTime departureTime,
        Integer stopoverTime
) {

    public static TrainStopResponse from(TrainTripStopRow row) {
        return new TrainStopResponse(
                String.valueOf(row.getStopSequence()),
                row.getStationName(),
                row.getArrivalTime(),
                row.getDepartureTime(),
                stopoverMinutes(row)
        );
    }

    private static Integer stopoverMinutes(TrainTripStopRow row) {
        // Terminal stops stay at one end of the trip, so they have no stopover.
        if(row.getArrivalTime() == null || row.getDepartureTime() == null) {
            return null;
        }
        return (int) Duration.between(
                row.getArrivalTime(), row.getDepartureTime()
        ).toMinutes();
    }
}
