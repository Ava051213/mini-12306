package com.mia.mini12306.train;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class StationCodeNotFoundException extends RuntimeException {

    public StationCodeNotFoundException() {
        super("出发地或目的地不存在");
    }
}
