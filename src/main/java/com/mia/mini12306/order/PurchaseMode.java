package com.mia.mini12306.order;

public enum PurchaseMode {

    // Orders created by the old entry, without any price snapshot.
    LEGACY,

    // Orders priced by the server from the full-route interval price.
    PRICED,

    // Orders that hold concrete seat segments for the purchased interval.
    SEAT
}
