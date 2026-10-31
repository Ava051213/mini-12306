package com.mia.mini12306.order;

public enum PurchaseMode {

    // Orders created by the old entry, without any price snapshot.
    LEGACY,

    // Orders priced by the server from the full-route interval price.
    PRICED
}
