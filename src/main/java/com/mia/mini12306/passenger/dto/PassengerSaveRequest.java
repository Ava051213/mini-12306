package com.mia.mini12306.passenger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PassengerSaveRequest(
        @NotBlank
        @Size(min = 2, max = 50)
        String realName,

        @NotBlank
        @Pattern(regexp = "^[0-9]{17}[0-9Xx]$")
        String idCard,

        @NotBlank
        @Pattern(regexp = "^1[3-9][0-9]{9}$")
        String phone
) {

}
