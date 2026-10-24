package com.mia.mini12306.order.dto;

import com.mia.mini12306.order.OrderStatus;
import com.mia.mini12306.order.TicketOrder;

import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long trainTripId,
        Long passengerId,
        String passengerName,
        OrderStatus status,
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
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
