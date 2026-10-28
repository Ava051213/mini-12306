package com.mia.mini12306.order;

// Keep the original request parameters for retry comparison.
public record OrderIdempotency(
    Long userId,
    String idempotencyKey,
    Long trainTripId,
    Long passengerId,
    Long orderId
) {

}
