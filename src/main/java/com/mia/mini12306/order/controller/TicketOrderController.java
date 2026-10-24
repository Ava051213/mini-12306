package com.mia.mini12306.order.controller;

import com.mia.mini12306.order.TicketOrder;
import com.mia.mini12306.order.TicketOrderService;
import com.mia.mini12306.order.dto.OrderCreateRequest;
import com.mia.mini12306.order.dto.OrderCreateResponse;
import com.mia.mini12306.order.dto.OrderResponse;
import com.mia.mini12306.user.CurrentUserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public List<OrderResponse> findAll(
            HttpServletRequest httpRequest
    ) {
        // Always derive the owner from the authenticated session.
        Long userId = currentUserSession.requireUser(httpRequest).id();

        return ticketOrderService.findAll(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public OrderResponse findById(
            @PathVariable @Positive Long id,
            HttpServletRequest httpRequest
    ) {
        Long userId = currentUserSession.requireUser(httpRequest).id();

        return OrderResponse.from(
                ticketOrderService.findById(userId, id)
        );
    }

    @PostMapping("/{id}/pay")
    public OrderResponse pay(
            @PathVariable @Positive Long id,
            HttpServletRequest httpRequest
    ) {
        // Derive ownership from the session, never from request data.
        Long userId = currentUserSession.requireUser(httpRequest).id();

        return OrderResponse.from(
                ticketOrderService.pay(userId, id)
        );
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(
            @PathVariable @Positive Long id,
            HttpServletRequest httpRequest
    ) {
        Long userId = currentUserSession.requireUser(httpRequest).id();

        // The service transaction includes cancellation and stock restoration.
        return OrderResponse.from(
                ticketOrderService.cancel(userId, id)
        );
    }
}
