package com.mia.mini12306.inventory;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TicketInventory {

    private Long trainTripId;
    private Integer totalTickets;
    private Integer remainingTickets;
}
