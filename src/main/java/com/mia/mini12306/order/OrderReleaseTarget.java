package com.mia.mini12306.order;

// Minimal facts needed to release what one order still holds.
public record OrderReleaseTarget(
        Long id,
        PurchaseMode purchaseMode,
        Long trainTripId,
        Integer departureSequence,
        Integer arrivalSequence
) {
}
