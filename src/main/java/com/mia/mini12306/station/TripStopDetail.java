package com.mia.mini12306.station;

import java.time.LocalDateTime;

// One stop of a run, with the station code used by purchase requests.
public record TripStopDetail(
        int stopSequence,
        String stationCode,
        String stationName,
        LocalDateTime arrivalAt,
        LocalDateTime departureAt
) {

}
