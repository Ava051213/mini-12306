package com.mia.mini12306.station;

import com.mia.mini12306.station.dto.TrainStopResponse;

public class TrainTripNotFoundException extends RuntimeException {

    public TrainTripNotFoundException() {
        super("车次不存在");
    }
}
