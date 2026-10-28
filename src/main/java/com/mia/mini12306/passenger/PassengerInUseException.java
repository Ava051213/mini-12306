package com.mia.mini12306.passenger;

public class PassengerInUseException extends RuntimeException {

    public PassengerInUseException() {
        super("该乘车人已被订单引用，无法删除");
    }
}
