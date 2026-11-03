package com.mia.mini12306.order;

public class SeatNotAvailableException extends RuntimeException {

    private SeatNotAvailableException(String message) {
        super(message);
    }

    public static SeatNotAvailableException notConfigured() {
        return new SeatNotAvailableException("该席别未配置座位");
    }

    public static SeatNotAvailableException soldOut() {
        return new SeatNotAvailableException("该区间席别无可用座位");
    }
}
