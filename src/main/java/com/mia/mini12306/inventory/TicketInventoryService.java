package com.mia.mini12306.inventory;

import com.mia.mini12306.inventory.mapper.TicketInventoryMapper;
import org.springframework.stereotype.Service;

@Service
public class TicketInventoryService {

    private final TicketInventoryMapper ticketInventoryMapper;

    public TicketInventoryService(TicketInventoryMapper ticketInventoryMapper) {
        this.ticketInventoryMapper = ticketInventoryMapper;
    }

    public TicketInventory findByTrainTripId(Long trainTripId) {
        TicketInventory inventory =
                ticketInventoryMapper.findByTrainTripId(trainTripId);

        // Missing inventory is different from zero remaining tickets.
        if(inventory == null) {
            throw new TicketInventoryNotFoundException();
        }

        return inventory;
    }
}
