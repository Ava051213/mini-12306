package com.mia.mini12306.station;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TrainTripStopRow {

    private Integer stopSequence;
    private String stationName;
    private LocalDateTime arrivalTime;
    private LocalDateTime departureTime;
}
