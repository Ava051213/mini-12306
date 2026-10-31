package com.mia.mini12306.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;

public record PricedOrderCreateRequest(
    @NonNull
    @Positive
    Long trainTripId,

    @NotNull
    @Positive
    Long passengerId,

    @NotNull
    @Min(0)
    @Max(2)
    Integer seatType
) {
}
