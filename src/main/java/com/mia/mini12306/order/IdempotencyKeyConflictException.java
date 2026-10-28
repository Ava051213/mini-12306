package com.mia.mini12306.order;

public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException() {
        // Keep the message independent of submitted values.
        super("幂等键已用于不同的下单请求");
    }
}
