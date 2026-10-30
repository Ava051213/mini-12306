package com.mia.mini12306.train.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record TicketQueryRequest(
        // Filter by the boarding date, bound as yyyy-MM-dd.
        @NotNull
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate departureDate,

        // Station codes, not names, so the lookup stays unambiguous.
        @NotBlank
        String fromStation,

        @NotBlank
        String toStation
) {

}
