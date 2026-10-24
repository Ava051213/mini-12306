package com.mia.mini12306.inventory;

public class TicketInventoryNotFoundException extends RuntimeException {

    public TicketInventoryNotFoundException() {
        super("车次不存在或未配置库存");
    }
}
