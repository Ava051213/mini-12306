package com.mia.mini12306.order;

public enum OrderStatus {

    // New orders reserve stock and wait for payment.
    PENDING_PAYMENT,

    // Payment keeps the reserved stock.
    PAID,

    // Cancellation releases the reserved stock.
    CANCELLED,

    // The payment window closed before payment, so stock was released.
    EXPIRED
}
