package com.mia.mini12306.order.dto;

import com.mia.mini12306.order.OrderStatus;
import com.mia.mini12306.order.TicketOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long trainTripId,
        Long passengerId,
        String passengerName,
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    // Expose order details without the owner ID or identity document.
    public static OrderResponse from(TicketOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getTrainTripId(),
                order.getPassengerId(),
                order.getPassengerName(),
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
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    // Cents stay exact until the response boundary.
    private static BigDecimal toAmount(Integer amountCents) {
        return amountCents == null
                ? null
                : BigDecimal.valueOf(amountCents, 2);
    }
}
