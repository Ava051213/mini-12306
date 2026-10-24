package com.mia.mini12306.inventory;

public class InsufficientTicketsException extends RuntimeException {

    public InsufficientTicketsException() {
        super("余票不足");
    }
}
