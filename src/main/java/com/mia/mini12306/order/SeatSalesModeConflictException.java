package com.mia.mini12306.order;

public class SeatSalesModeConflictException extends RuntimeException {

    private SeatSalesModeConflictException(String message) {
        super(message);
    }

    public static SeatSalesModeConflictException seatSalesDisabled() {
        return new SeatSalesModeConflictException("该车次尚未启用座位售票");
    }

    public static SeatSalesModeConflictException seatSalesEnabled() {
        return new SeatSalesModeConflictException(
                "该车次已启用座位售票，请使用座位购票入口"
        );
    }
}
