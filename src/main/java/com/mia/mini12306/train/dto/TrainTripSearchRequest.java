package com.mia.mini12306.train.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record TrainTripSearchRequest(
        // Bind the query date in yyyy-MM-dd format.
        @NotNull
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate departureDate,

        @NotBlank
        String fromStation,

        @NotBlank
        String toStation
) {

}