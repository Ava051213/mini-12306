package com.mia.mini12306.user.dto;

import com.mia.mini12306.user.UserAccount;

import java.time.LocalDateTime;

public record UserRegisterResponse(
        Long id,
        String username,
        LocalDateTime createdAt
) {
   public static UserRegisterResponse from(UserAccount user) {
       return new UserRegisterResponse(
               user.getId(),
               user.getUsername(),
               user.getCreatedAt()
       );
   }
}
