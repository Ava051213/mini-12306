package com.mia.mini12306.inventory.controller;

import com.mia.mini12306.inventory.TicketInventoryService;
import com.mia.mini12306.inventory.dto.TicketInventoryResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trains")
public class TicketInventoryController {

    private final TicketInventoryService ticketInventoryService;

    public TicketInventoryController(
            TicketInventoryService ticketInventoryService) {
        this.ticketInventoryService = ticketInventoryService;
    }

    @GetMapping("/{trainTripId}/inventory")
    public TicketInventoryResponse findByTrainTripId(
            @PathVariable @Positive Long trainTripId
    ) {
        // Convert the database entity to the public response.
        return TicketInventoryResponse.from(
                ticketInventoryService.findByTrainTripId(trainTripId)
        );
    }
}
