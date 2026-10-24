package com.mia.mini12306.order;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException() {
        super("订单不存在");
    }
}
