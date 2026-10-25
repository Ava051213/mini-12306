package com.mia.mini12306.order.dto;

import java.util.List;

public record OrderPageResponse(
        List<OrderResponse> items,
        int page,
        int size,
        long total,
        long totalPages
) {

}
