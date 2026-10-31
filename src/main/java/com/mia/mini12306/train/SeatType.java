package com.mia.mini12306.train;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum SeatType {

    BUSINESS_CLASS(0, "商务座"),
    FIRST_CLASS(1, "一等座"),
    SECOND_CLASS(2, "二等座");

    private final int code;
    private final String label;

    // The table CHECK keeps other codes out, so this is the only supported set.
    public static Optional<SeatType> fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(each -> Objects.equals(each.code, code))
                .findFirst();
    }
}
