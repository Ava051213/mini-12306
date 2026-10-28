package com.mia.mini12306.order;

// Distinguish initial creation from a successful retry.
public record OrderCreateResult(
        TicketOrder order,
        boolean created
) {
}
