package com.mia.mini12306.user.controller;

import com.mia.mini12306.user.UserAccount;
import com.mia.mini12306.user.UserRegistrationService;
import com.mia.mini12306.user.dto.UserRegisterRequest;
import com.mia.mini12306.user.dto.UserRegisterResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRegistrationService registrationService;

    public UserController(UserRegistrationService registrationService) {
        this.registrationService = registrationService;
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
}
