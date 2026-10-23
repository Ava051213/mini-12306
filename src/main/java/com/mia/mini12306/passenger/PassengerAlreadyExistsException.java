package com.mia.mini12306.passenger;

public class PassengerAlreadyExistsException extends RuntimeException {

    public PassengerAlreadyExistsException() {
        super("该乘车人已添加");
    }
}
