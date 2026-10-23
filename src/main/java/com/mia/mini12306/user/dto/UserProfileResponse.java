package com.mia.mini12306.user.dto;

import com.mia.mini12306.user.UserAccount;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        LocalDateTime createdAt
) {

    public static UserProfileResponse from(UserAccount user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getCreatedAt()
        );
    }
}
