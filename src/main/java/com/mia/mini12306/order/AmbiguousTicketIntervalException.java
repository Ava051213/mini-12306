package com.mia.mini12306.order;

public class AmbiguousTicketIntervalException extends RuntimeException {

    public AmbiguousTicketIntervalException() {
        super("该车次多次经过所购车站，暂不支持购票");
    }
}
