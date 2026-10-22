package com.mia.mini12306.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

public record UserRegisterRequest (
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_]{3,50}$")
    String username,

    @NotBlank
    @Size(min = 8, max = 72)
    String password
) {

    @AssertTrue(message = "password must not exceed 72 UTF-8 bytes")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null
                || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
