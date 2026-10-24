package com.mia.mini12306.train;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTrainTripSearchException extends RuntimeException {

    public InvalidTrainTripSearchException() {
        super("起点和终点不能相同");
    }
}
