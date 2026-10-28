package com.mia.mini12306.order.dto;

import com.mia.mini12306.order.OrderStatus;
import com.mia.mini12306.order.TicketOrder;

import java.time.LocalDateTime;

public record OrderCreateResponse(
        Long id,
        Long trainTripId,
        Long passengerId,
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
                order.getStatus(),
                order.getPayDeadline(),
                order.getCreatedAt()
        );
    }
}
