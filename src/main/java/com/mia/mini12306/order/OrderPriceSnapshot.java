package com.mia.mini12306.order;

import java.time.LocalDateTime;

// Immutable full-route fare read while creating one priced order.
public record OrderPriceSnapshot(
        int departureSequence,
        int arrivalSequence,
        String departureStationCode,
        String departureStationName,
        LocalDateTime departureAt,
        String arrivalStationCode,
        String arrivalStationName,
        LocalDateTime arrivalAt,
        int seatType,
        int amountCents
) {
}
