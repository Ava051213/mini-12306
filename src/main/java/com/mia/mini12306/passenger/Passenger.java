package com.mia.mini12306.passenger;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Passenger {

    private Long id;
    private Long userId;
    private String realName;
    private String idCard;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
