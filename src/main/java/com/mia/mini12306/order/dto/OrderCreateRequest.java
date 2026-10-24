package com.mia.mini12306.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderCreateRequest(
        @NotNull
        @Positive
        Long trainTripId,

        @NotNull
        @Positive
        Long passengerId
) {
}
