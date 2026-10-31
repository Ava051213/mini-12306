package com.mia.mini12306.station;

import java.time.LocalDateTime;

// First or last stop of a trip; a valid full route returns exactly two rows.
public record RouteBoundaryStop(
        int stopSequence,
        String stationCode,
        String stationName,
        LocalDateTime arrivalAt,
        LocalDateTime departureAt
) {
}
