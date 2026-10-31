package com.mia.mini12306.order;

public class FullRouteStopsIncompleteException extends RuntimeException {

    public FullRouteStopsIncompleteException() {
        super("该车次全程经停资料不完整，无法下单");
    }
}
