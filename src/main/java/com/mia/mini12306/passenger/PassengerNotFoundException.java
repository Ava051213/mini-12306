package com.mia.mini12306.passenger;

public class PassengerNotFoundException extends RuntimeException {

    public PassengerNotFoundException() {
        super("乘车人不存在");
    }
}
