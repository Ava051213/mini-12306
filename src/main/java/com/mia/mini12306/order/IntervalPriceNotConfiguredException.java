package com.mia.mini12306.order;

public class IntervalPriceNotConfiguredException extends RuntimeException {

    public IntervalPriceNotConfiguredException() {
        super("该区间席别未配置票价");
    }
}
