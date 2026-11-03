package com.mia.mini12306.order;

public class InvalidTicketIntervalException extends RuntimeException {

    public InvalidTicketIntervalException() {
        super("上下车站不构成该车次的有效区间");
    }
}
