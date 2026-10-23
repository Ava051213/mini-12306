package com.mia.mini12306.user.controller;

import com.mia.mini12306.user.*;
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

    private final UserRegistrationService registrationService;
    private final UserLoginService loginService;
    private final CurrentUserSession currentUserSession;

    public UserController(
            UserRegistrationService registrationService,
            UserLoginService loginService,
            CurrentUserSession currentUserSession
    ) {
        this.registrationService = registrationService;
        this.loginService = loginService;
        this.currentUserSession = currentUserSession;
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
        session.setAttribute(CurrentUserSession.LOGIN_USER_ID, user.id());

        return user;
    }

    @GetMapping("/me")
    public UserProfileResponse me(HttpServletRequest httpRequest) {
        return currentUserSession.requireUser(httpRequest);
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
