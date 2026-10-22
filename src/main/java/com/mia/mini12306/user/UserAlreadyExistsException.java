package com.mia.mini12306.user;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String username) {
        super("用户名已存在: " + username);
    }
}
