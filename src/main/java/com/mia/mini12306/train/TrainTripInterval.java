package com.mia.mini12306.train;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TrainTripInterval {

    private Long trainTripId;
    private String trainNumber;
    private String departureStation;
    private String arrivalStation;
    private LocalDateTime departureAt;
    private LocalDateTime arrivalAt;
    private Integer departureSequence;
    private Integer arrivalSequence;
}
