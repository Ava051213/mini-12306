package com.mia.mini12306.user;

import com.mia.mini12306.user.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserSession {

    public static final String LOGIN_USER_ID = "LOGIN_USER_ID";

    private final UserLoginService loginService;

    public CurrentUserSession(UserLoginService loginService) {
        this.loginService = loginService;
    }

    public UserProfileResponse requireUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if(session == null) {
            throw new NotLoggedInException();
        }

        Object value = session.getAttribute(LOGIN_USER_ID);

        if(!(value instanceof Long userId)) {
            throw new NotLoggedInException();
        }

        return loginService.getCurrentUser(userId);
    }
}
