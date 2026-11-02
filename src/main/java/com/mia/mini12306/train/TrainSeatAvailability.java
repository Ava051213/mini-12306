package com.mia.mini12306.train;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrainSeatAvailability {

    private Long trainTripId;
    private Integer departureSequence;
    private Integer arrivalSequence;
    private Integer seatType;
    private Integer availableSeats;
}
