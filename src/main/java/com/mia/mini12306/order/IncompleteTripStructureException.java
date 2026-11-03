package com.mia.mini12306.order;

public class IncompleteTripStructureException extends RuntimeException {

    public IncompleteTripStructureException() {
        super("该车次经停或座位资料不完整，暂不支持区间购票");
    }
}
