package com.mia.mini12306.inventory.dto;

import com.mia.mini12306.inventory.TicketInventory;

public record TicketInventoryResponse(
        Long trainTripId,
        Integer remainingTickets
) {

    // Expose only the fields needed for ticket availability.
    public static TicketInventoryResponse from(
            TicketInventory inventory
    ) {
        return new TicketInventoryResponse(
                inventory.getTrainTripId(),
                inventory.getRemainingTickets()
        );
    }
}
