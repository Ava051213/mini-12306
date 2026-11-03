package com.mia.mini12306.order.dto;

import jakarta.validation.constraints.*;

public record SeatOrderCreateRequest(
        @NotNull
        @Positive
        Long trainTripId,

        @NotNull
        @Positive
        Long passengerId,

        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9]{1,20}")
        String fromStation,

        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9]{1,20}")
        String toStation,

        @NotNull
        @Min(0)
        @Max(2)
        Integer seatType
) {
}
