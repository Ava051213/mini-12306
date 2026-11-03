package com.mia.mini12306.order.dto;

import com.mia.mini12306.order.OrderStatus;
import com.mia.mini12306.order.TicketOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreateResponse(
        Long id,
        Long trainTripId,
        Long passengerId,
        String departureStationCode,
        String departureStationName,
        LocalDateTime departureAt,
        String arrivalStationCode,
        String arrivalStationName,
        LocalDateTime arrivalAt,
        Integer seatType,
        String carriageNumber,
        String seatNumber,
        BigDecimal amount,
        OrderStatus status,
        LocalDateTime payDeadline,
        LocalDateTime createdAt
) {

    // Expose only the fields needed for the creation result.
    public static OrderCreateResponse from(TicketOrder order) {
        return new OrderCreateResponse(
                order.getId(),
                order.getTrainTripId(),
                order.getPassengerId(),
                order.getDepartureStationCode(),
                order.getDepartureStationName(),
                order.getDepartureAt(),
                order.getArrivalStationCode(),
                order.getArrivalStationName(),
                order.getArrivalAt(),
                order.getSeatType(),
                order.getCarriageNumber(),
                order.getSeatNumber(),
                toAmount(order.getAmountCents()),
                order.getStatus(),
                order.getPayDeadline(),
                order.getCreatedAt()
        );
    }

    // Cents stay exact until the response boundary.
    private static BigDecimal toAmount(Integer amountCents) {
        return amountCents == null
                ? null
                : BigDecimal.valueOf(amountCents, 2);
    }
}
