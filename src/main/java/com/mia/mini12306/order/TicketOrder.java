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

    // LEGACY keeps the old entry distinguishable from priced orders.
    private PurchaseMode purchaseMode;

    // Full-route fare snapshot; every field stays null for legacy orders.
    private Integer departureSequence;
    private Integer arrivalSequence;
    private String departureStationCode;
    private String departureStationName;
    private LocalDateTime departureAt;
    private String arrivalStationCode;
    private String arrivalStationName;
    private LocalDateTime arrivalAt;
    private Integer seatType;
    private Integer amountCents;

    private OrderStatus status;
    // Deadline decides whether the order may still be paid.
    private LocalDateTime payDeadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
