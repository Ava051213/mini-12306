package com.mia.mini12306.order;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TicketOrder {

    private Long id;
    private Long userId;
    private Long trainTripId;
    private Long passengerId;

    // Preserve passenger details as they were when the order was created.
    private String passengerName;
    private String passengerIdCard;

    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
