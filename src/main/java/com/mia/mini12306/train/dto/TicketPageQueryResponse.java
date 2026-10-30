package com.mia.mini12306.train.dto;

import java.util.List;

public record TicketPageQueryResponse(
        List<TicketListResponse> trainList
) {

}
