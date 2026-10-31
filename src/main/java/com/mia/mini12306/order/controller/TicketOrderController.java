package com.mia.mini12306.order.controller;

import com.mia.mini12306.order.OrderCreateResult;
import com.mia.mini12306.order.TicketOrder;
import com.mia.mini12306.order.TicketOrderService;
import com.mia.mini12306.order.dto.*;
import com.mia.mini12306.user.CurrentUserSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<OrderCreateResponse> create(
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9_-]{1,64}")
            String idempotencyKey,

            @Valid @RequestBody OrderCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        // Derive ownership from the authenticated session.
        Long userId = currentUserSession.requireUser(httpRequest).id();

        OrderCreateResult result = ticketOrderService.create(
                userId, idempotencyKey, request
        );

        // Select the HTTP status without exposing the internal result.
        HttpStatus status = result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status)
                .body(OrderCreateResponse.from(result.order()));
    }

    @PostMapping("/priced")
    public ResponseEntity<OrderCreateResponse> createPriced(
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9_-]{1,64}")
            String idempotencyKey,

            @Valid @RequestBody PricedOrderCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        // The server decides the price; the body carries no amount.
        Long userId = currentUserSession.requireUser(httpRequest).id();

        OrderCreateResult result = ticketOrderService.createPriced(
                userId, idempotencyKey, request
        );

        HttpStatus status = result.created()
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status)
                .body(OrderCreateResponse.from(result.order()));
    }

    @GetMapping
    public OrderPageResponse findPage(
            @RequestParam(name = "page", defaultValue = "1")
            @Min(1) int page,

            @RequestParam(name = "size", defaultValue = "10")
            @Min(1) @Max(100) int size,

            HttpServletRequest httpRequest
    ) {
        // Always derive ownership from the authenticated session.
        Long userId = currentUserSession.requireUser(httpRequest).id();

        return ticketOrderService.findPage(userId, page, size);
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
