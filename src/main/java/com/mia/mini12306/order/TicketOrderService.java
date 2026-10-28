package com.mia.mini12306.order;

import com.mia.mini12306.inventory.InsufficientTicketsException;
import com.mia.mini12306.inventory.TicketInventoryService;
import com.mia.mini12306.inventory.mapper.TicketInventoryMapper;
import com.mia.mini12306.order.dto.OrderCreateRequest;
import com.mia.mini12306.order.dto.OrderPageResponse;
import com.mia.mini12306.order.dto.OrderResponse;
import com.mia.mini12306.order.mapper.OrderIdempotencyMapper;
import com.mia.mini12306.order.mapper.TicketOrderMapper;
import com.mia.mini12306.passenger.Passenger;
import com.mia.mini12306.passenger.PassengerService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketOrderService {

    private final PassengerService passengerService;
    private final TicketInventoryService ticketInventoryService;
    private final TicketInventoryMapper ticketInventoryMapper;
    private final TicketOrderMapper ticketOrderMapper;
    private final OrderIdempotencyMapper orderIdempotencyMapper;

    public TicketOrderService(
            PassengerService passengerService,
            TicketInventoryService ticketInventoryService,
            TicketInventoryMapper ticketInventoryMapper,
            TicketOrderMapper ticketOrderMapper,
            OrderIdempotencyMapper OrderIdempotencyMapper
    ) {
        this.passengerService = passengerService;
        this.ticketInventoryService = ticketInventoryService;
        this.ticketInventoryMapper = ticketInventoryMapper;
        this.ticketOrderMapper = ticketOrderMapper;
        this.orderIdempotencyMapper = OrderIdempotencyMapper;
    }

    @Transactional
    public OrderCreateResult create(
            Long userId,
            String idempotencyKey,
            OrderCreateRequest request
    ) {
        // Serialize requests with the same account and key.
        orderIdempotencyMapper.reserve(
                userId,
                idempotencyKey,
                request.trainTripId(),
                request.passengerId()
        );

        OrderIdempotency idempotency = orderIdempotencyMapper.findForUpdate(
                userId, idempotencyKey
        );

        if(idempotency == null) {
            throw new IllegalStateException("Idempotency record could not be loaded");
        }

        // Compare the original parameters before checking current business state.
        if(!idempotency.trainTripId().equals(request.trainTripId())
                || !idempotency.passengerId().equals(request.passengerId())) {
            throw new IdempotencyKeyConflictException();
        }

        if(idempotency.orderId() != null) {
            // Return the existing order without changing stock.
            TicketOrder existing = ticketOrderMapper.findByIdAndUserIdForUpdate(
                    idempotency.orderId(), userId
            );

            if(existing == null) {
                throw new IllegalStateException("Idempotent order could not be loaded");
            }

            return new OrderCreateResult(existing, false);
        }

        // Verify ownership before changing stock.
        Passenger passenger = passengerService.findById(
                userId, request.passengerId()
        );

        ticketInventoryService.findByTrainTripId(request.trainTripId());

        // Only the conditional update decides whether stock is available.
        int affectedRows = ticketInventoryMapper.decrementIfAvailable(
                request.trainTripId()
        );

        if(affectedRows == 0) {
            throw new InsufficientTicketsException();
        }

        // Build trusted order data on the server.
        TicketOrder order = new TicketOrder();
        order.setUserId(userId);
        order.setTrainTripId(request.trainTripId());
        order.setPassengerId(passenger.getId());
        order.setPassengerName(passenger.getRealName());
        order.setPassengerIdCard(passenger.getIdCard());
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        if(ticketOrderMapper.insert(order) != 1) {
            throw new IllegalStateException("Order insert failed");
        }

        TicketOrder saved = ticketOrderMapper.findByIdAndUserId(
                order.getId(), userId
        );

        if(saved == null) {
            throw new IllegalStateException("Created order could not be loaded");
        }

        // Commit the mapping together with the order and stock decrease.
        int attachedRows = orderIdempotencyMapper.attachOrder(
                userId, idempotencyKey, saved.getId()
        );

        if(attachedRows != 1) {
            throw new IllegalStateException("Idempotency order association failed");
        }

        return new OrderCreateResult(saved, true);
    }

    public OrderPageResponse findPage(Long userId, int page, int size) {
        // Use long arithmetic before multiplying.
        long offset = ((long) page - 1) * size;

        long total = ticketOrderMapper.countByUserId(userId);

        // Map only the rows returned for this page.
        List<OrderResponse> items = ticketOrderMapper
                .findPageByUserId(userId, size, offset)
                .stream()
                .map(OrderResponse::from)
                .toList();

        // Round up without adding to the total.
        long totalPages = total / size + (total % size == 0 ? 0 : 1);

        return new OrderPageResponse(
                items,
                page,
                size,
                total,
                totalPages
        );
    }

    public TicketOrder findById(Long userId, Long id) {
        // Apply ownership in the database query.
        TicketOrder order = ticketOrderMapper.findByIdAndUserId(id, userId);

        // Hide whether the order belongs to another account.
        if(order == null) {
            throw new OrderNotFoundException();
        }

        return order;
    }

    @Transactional
    public TicketOrder pay(Long userId, Long id) {
        // Payment is allowed only inside the payment window.
        int affectedRows = ticketOrderMapper.updateStatusIfPendingAndNotExpired(
                id, userId
        );

        TicketOrder order = findByIdForUpdate(userId, id);

        if(affectedRows == 0) {
            // Repeating a successful payment stays idempotent.
            if(order.getStatus() == OrderStatus.PAID) {
                return order;
            }

            // Pending means the deadline blocked it; expired means already closed.
            if(order.getStatus() == OrderStatus.PENDING_PAYMENT
                    || order.getStatus() == OrderStatus.EXPIRED) {
                throw new OrderStateConflictException("订单已超时关闭，无法支付");
            }

            throw new OrderStateConflictException("当前订单状态不允许支付");
        }

        return order;
    }

    @Transactional
    public TicketOrder cancel(Long userId, Long id) {
        int affectedRows = ticketOrderMapper.updateStatusIfPending(
                id, userId, OrderStatus.CANCELLED
        );

        TicketOrder order = findByIdForUpdate(userId, id);

        if(affectedRows == 0) {
            // A repeated cancellation must not restore stock again.
            if(order.getStatus() == OrderStatus.CANCELLED) {
                return order;
            }

            throw new OrderStateConflictException("当前订单状态不允许取消");
        }

        // Restore stock only for the request that changed the order state.
        int restoredRows = ticketInventoryMapper.restoreOneIfNotFull(
                order.getTrainTripId()
        );

        if(restoredRows != 1) {
            // Roll back the order transition if stock cannot be restored.
            throw new IllegalStateException("Cancelled order inventory could not be restored");
        }

        return order;
    }

    @Transactional
    public boolean closeExpired(Long orderId) {
        // Only the caller that actually changes the status may release stock.
        int affectedRows = ticketOrderMapper.expireIfPendingAndOverdue(orderId);

        if(affectedRows == 0) {
            // Already paid, cancelled, closed, or not yet overdue.
            return false;
        }

        Long trainTripId = ticketOrderMapper.findTrainTripIdById(orderId);

        if(trainTripId == null) {
            throw new IllegalStateException("Expired order could not be loaded");
        }

        int restoredRows = ticketInventoryMapper.restoreOneIfNotFull(trainTripId);

        if(restoredRows != 1) {
            // Roll back the status change if stock cannot be restored.
            throw new IllegalStateException("Expired order inventory could not be restored");
        }

        return true;
    }

    private TicketOrder findByIdForUpdate(Long userId, Long id) {
        // Read the latest owned order within the caller's transaction.
        TicketOrder order = ticketOrderMapper.findByIdAndUserIdForUpdate(id, userId);

        if(order == null) {
            throw new OrderNotFoundException();
        }

        return order;
    }
}
