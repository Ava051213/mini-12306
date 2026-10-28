package com.mia.mini12306.order;

import com.mia.mini12306.order.mapper.TicketOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(
        name = "order.timeout-close.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OrderTimeoutCloseScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(OrderTimeoutCloseScheduler.class);

    // Bound the work of a single run.
    private static final int BATCH_SIZE = 100;

    private final TicketOrderMapper ticketOrderMapper;
    private final TicketOrderService ticketOrderService;

    public OrderTimeoutCloseScheduler(
            TicketOrderMapper ticketOrderMapper,
            TicketOrderService ticketOrderService
    ) {
        this.ticketOrderMapper = ticketOrderMapper;
        this.ticketOrderService = ticketOrderService;
    }

    @Scheduled(fixedDelayString = "${order.timeout-close.interval-ms:60000}")
    public void closeExpiredOrders() {
        List<Long> expiredIds = ticketOrderMapper.findExpiredIds(BATCH_SIZE);

        for(Long orderId : expiredIds) {
            try {
                // Each order commits or rolls back on its own.
                ticketOrderService.closeExpired(orderId);
            } catch(RuntimeException exception) {
                // Leave the order for the next run instead of failing the batch.
                log.warn("Failed to close expired order {}", orderId, exception);
            }
        }
    }
}
