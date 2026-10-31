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
        Integer seatType,
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
                order.getSeatType(),
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
