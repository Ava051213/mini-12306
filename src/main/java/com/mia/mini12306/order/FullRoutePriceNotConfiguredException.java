package com.mia.mini12306.order;

public class FullRoutePriceNotConfiguredException extends RuntimeException {

    public FullRoutePriceNotConfiguredException() {
        super("该车次全程席别未配置票价");
    }
}
