package com.mia.mini12306.user.controller;

import com.mia.mini12306.user.NotLoggedInException;
import com.mia.mini12306.user.UserAccount;
import com.mia.mini12306.user.UserLoginService;
import com.mia.mini12306.user.UserRegistrationService;
import com.mia.mini12306.user.dto.UserLoginRequest;
import com.mia.mini12306.user.dto.UserProfileResponse;
import com.mia.mini12306.user.dto.UserRegisterRequest;
import com.mia.mini12306.user.dto.UserRegisterResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String LOGIN_USER_ID = "LOGIN_USER_ID";

    private final UserRegistrationService registrationService;
    private final UserLoginService loginService;

    public UserController(
            UserRegistrationService registrationService,
            UserLoginService loginService
    ) {
        this.registrationService = registrationService;
        this.loginService = loginService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserRegisterResponse register(
            @Valid @RequestBody UserRegisterRequest request
    ) {
        UserAccount user = registrationService.register(
                request.username(),
                request.password()
        );
        return UserRegisterResponse.from(user);
    }

    @PostMapping("/login")
    public UserProfileResponse login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        UserProfileResponse user = loginService.authenticate(
                request.username(),
                request.password()
        );

        HttpSession oldSession = httpRequest.getSession(false);
        if(oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = httpRequest.getSession(true);
        session.setMaxInactiveInterval(30 * 60);
        session.setAttribute(LOGIN_USER_ID, user.id());

        return user;
    }

    @GetMapping("/me")
    public UserProfileResponse me(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);

        if(session == null) {
            throw new NotLoggedInException();
        }

        Object userId = session.getAttribute(LOGIN_USER_ID);

        if(!(userId instanceof Long)) {
            throw new NotLoggedInException();
        }

        return loginService.getCurrentUser((Long) userId);
    }

    @PostMapping("logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);

        if(session != null) {
            session.invalidate();
        }
    }
}
