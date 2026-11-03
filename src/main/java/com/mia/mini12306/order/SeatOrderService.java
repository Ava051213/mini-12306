package com.mia.mini12306.order;

import com.mia.mini12306.inventory.SeatAllocationService;
import com.mia.mini12306.inventory.SeatCandidate;
import com.mia.mini12306.order.dto.SeatOrderCreateRequest;
import com.mia.mini12306.order.mapper.OrderIdempotencyMapper;
import com.mia.mini12306.order.mapper.TicketOrderMapper;
import com.mia.mini12306.passenger.Passenger;
import com.mia.mini12306.passenger.PassengerService;
import com.mia.mini12306.station.TrainTripNotFoundException;
import com.mia.mini12306.train.mapper.TrainTripMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
public class SeatOrderService {

    private static final String SEAT_SALES_MODE = "SEAT";

    private final PassengerService passengerService;
    private final SeatAllocationService seatAllocationService;
    private final TicketOrderMapper ticketOrderMapper;
    private final OrderIdempotencyMapper orderIdempotencyMapper;
    private final OrderPricingService orderPricingService;
    private final TrainTripMapper trainTripMapper;

    public SeatOrderService(
            PassengerService passengerService,
            SeatAllocationService seatAllocationService,
            TicketOrderMapper ticketOrderMapper,
            OrderIdempotencyMapper orderIdempotencyMapper,
            OrderPricingService orderPricingService,
            TrainTripMapper trainTripMapper
    ) {
        this.passengerService = passengerService;
        this.seatAllocationService = seatAllocationService;
        this.ticketOrderMapper = ticketOrderMapper;
        this.orderIdempotencyMapper = orderIdempotencyMapper;
        this.orderPricingService = orderPricingService;
        this.trainTripMapper = trainTripMapper;
    }

    @Transactional
    public OrderCreateResult create(
            Long userId,
            String idempotencyKey,
            SeatOrderCreateRequest request
    ) {
        // Store and compare codes in one form, as the station table is case-insensitive.
        String fromCode = request.fromStation().toUpperCase(Locale.ROOT);
        String toCode = request.toStation().toUpperCase(Locale.ROOT);

        // Serialize requests with the same account and key.
        orderIdempotencyMapper.reserve(
                userId, idempotencyKey, PurchaseMode.SEAT,
                request.trainTripId(), request.passengerId(),
                request.seatType(), fromCode, toCode
        );

        OrderIdempotency idempotency = orderIdempotencyMapper.findForUpdate(
                userId, idempotencyKey
        );

        if(idempotency == null) {
            throw new IllegalStateException("Idempotency record could not be loaded");
        }

        if(!matches(idempotency, request, fromCode, toCode)) {
            throw new IdempotencyKeyConflictException();
        }

        if(idempotency.orderId() != null) {
            // A retry returns the stored order without touching seats or prices.
            TicketOrder existing = ticketOrderMapper.findByIdAndUserIdForUpdate(
                    idempotency.orderId(), userId
            );

            if(existing == null) {
                throw new IllegalStateException("Idempotent order could not be loaded");
            }

            return new OrderCreateResult(existing, false);
        }

        // Hold the run until this transaction commits.
        String salesMode = trainTripMapper.findSalesModeForUpdate(
                request.trainTripId()
        );

        if(salesMode == null) {
            throw new TrainTripNotFoundException();
        }

        if(!SEAT_SALES_MODE.equals(salesMode)) {
            throw SeatSalesModeConflictException.seatSalesDisabled();
        }

        // Verify ownership before reading the fare or touching seats.
        Passenger passenger = passengerService.findById(
                userId, request.passengerId()
        );

        // Read the fare exactly for this interval and seat type.
        OrderPriceSnapshot snapshot = orderPricingService.priceInterval(
                request.trainTripId(), fromCode, toCode, request.seatType()
        );

        // Pick one seat whose whole interval is free.
        SeatCandidate seat = seatAllocationService.allocate(
                request.trainTripId(),
                snapshot.seatType(),
                snapshot.departureSequence(),
                snapshot.arrivalSequence()
        );

        // Build trusted order data on the server.
        TicketOrder order = new TicketOrder();
        order.setUserId(userId);
        order.setTrainTripId(request.trainTripId());
        order.setPassengerId(passenger.getId());
        order.setPassengerName(passenger.getRealName());
        order.setPassengerIdCard(passenger.getIdCard());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPurchaseMode(PurchaseMode.SEAT);
        order.setDepartureSequence(snapshot.departureSequence());
        order.setArrivalSequence(snapshot.arrivalSequence());
        order.setDepartureStationCode(snapshot.departureStationCode());
        order.setDepartureStationName(snapshot.departureStationName());
        order.setDepartureAt(snapshot.departureAt());
        order.setArrivalStationCode(snapshot.arrivalStationCode());
        order.setArrivalStationName(snapshot.arrivalStationName());
        order.setArrivalAt(snapshot.arrivalAt());
        order.setSeatType(snapshot.seatType());
        order.setAmountCents(snapshot.amountCents());
        order.setSeatId(seat.seatId());
        order.setCarriageNumber(seat.carriageNumber());
        order.setSeatNumber(seat.seatNumber());

        if(ticketOrderMapper.insert(order) != 1) {
            throw new IllegalStateException("Order insert failed");
        }

        // Occupy after the insert so every segment can point at this order.
        seatAllocationService.occupy(
                request.trainTripId(),
                seat.seatId(),
                order.getId(),
                snapshot.departureSequence(),
                snapshot.arrivalSequence()
        );

        TicketOrder saved = ticketOrderMapper.findByIdAndUserId(
                order.getId(), userId
        );

        if(saved == null) {
            throw new IllegalStateException("Created order could not be loaded");
        }

        // Commit the mapping together with the order and the seat segments.
        if(orderIdempotencyMapper.attachOrder(
                userId, idempotencyKey, saved.getId()
        ) != 1) {
            throw new IllegalStateException("Idempotency order association failed");
        }

        return new OrderCreateResult(saved, true);
    }

    private boolean matches(
            OrderIdempotency idempotency,
            SeatOrderCreateRequest request,
            String fromCode,
            String toCode
    ) {
        return idempotency.purchaseMode() == PurchaseMode.SEAT
                && idempotency.trainTripId().equals(request.trainTripId())
                && idempotency.passengerId().equals(request.passengerId())
                && Objects.equals(idempotency.seatType(), request.seatType())
                && fromCode.equals(idempotency.departureStationCode())
                && toCode.equals(idempotency.arrivalStationCode());
    }
}
