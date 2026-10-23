package com.mia.mini12306.passenger.dto;

import com.mia.mini12306.passenger.Passenger;

import java.time.LocalDateTime;

public record PassengerResponse(
        Long id,
        String realName,
        String idCard,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PassengerResponse from(Passenger passenger) {
        return new PassengerResponse(
                passenger.getId(),
                passenger.getRealName(),
                passenger.getIdCard(),
                passenger.getPhone(),
                passenger.getCreatedAt(),
                passenger.getUpdatedAt()
        );
    }
}
