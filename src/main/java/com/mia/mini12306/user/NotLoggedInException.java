package com.mia.mini12306.user;

public class NotLoggedInException extends RuntimeException {

    public NotLoggedInException() {
        super("请先登录");
    }
}
