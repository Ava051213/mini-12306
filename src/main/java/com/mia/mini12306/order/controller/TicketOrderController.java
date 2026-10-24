package com.mia.mini12306.order.controller;

import com.mia.mini12306.order.TicketOrder;
import com.mia.mini12306.order.TicketOrderService;
import com.mia.mini12306.order.dto.OrderCreateRequest;
import com.mia.mini12306.order.dto.OrderCreateResponse;
import com.mia.mini12306.user.CurrentUserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class TicketOrderController {

    private final TicketOrderService ticketOrderService;
    private final CurrentUserSession currentUserSession;

    public TicketOrderController(
            TicketOrderService ticketOrderService,
            CurrentUserSession currentUserSession
    ) {
        this.ticketOrderService = ticketOrderService;
        this.currentUserSession = currentUserSession;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderCreateResponse create(
            @Valid @RequestBody OrderCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        // Derive the order owner from the server-side session.
        Long userId = currentUserSession.requireUser(httpRequest).id();

        // Call the injected service through Spring's transaction proxy.
        TicketOrder order = ticketOrderService.create(userId, request);
        return OrderCreateResponse.from(order);
    }
}
