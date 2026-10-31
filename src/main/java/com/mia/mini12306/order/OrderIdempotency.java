package com.mia.mini12306.order;

// Keep the original request parameters for retry comparison.
public record OrderIdempotency(
    Long userId,
    String idempotencyKey,
    PurchaseMode purchaseMode,
    Long trainTripId,
    Long passengerId,
    Integer seatType,
    Long orderId
) {
}
