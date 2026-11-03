package com.mia.mini12306.order;

import com.mia.mini12306.order.dto.OrderCreateRequest;
import com.mia.mini12306.order.dto.OrderCreateResponse;
import com.mia.mini12306.order.dto.OrderPageResponse;
import com.mia.mini12306.order.dto.OrderResponse;
import com.mia.mini12306.order.dto.PricedOrderCreateRequest;
import com.mia.mini12306.order.dto.SeatOrderCreateRequest;
import com.mia.mini12306.order.mapper.OrderIdempotencyMapper;
import com.mia.mini12306.passenger.dto.PassengerResponse;
import com.mia.mini12306.passenger.dto.PassengerSaveRequest;
import com.mia.mini12306.support.DatabaseIntegrationTest;
import com.mia.mini12306.train.dto.SeatClassResponse;
import com.mia.mini12306.train.dto.TicketPageQueryResponse;
import com.mia.mini12306.user.CurrentUserSession;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SeatOrderIntegrationTests extends DatabaseIntegrationTest {

    private static final String SEAT_ORDERS = "/api/orders/seat";
    private static final String LEGACY_ORDERS = "/api/orders";
    private static final String PRICED_ORDERS = "/api/orders/priced";
    private static final String TICKET_QUERY = "/api/ticket-service/ticket/query";
    private static final LocalDateTime BASE_DEPARTURE =
            LocalDateTime.of(2099, 1, 1, 8, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SeatOrderService seatOrderService;

    @Autowired
    private TicketOrderService ticketOrderService;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @MockitoSpyBean
    private OrderIdempotencyMapper orderIdempotencyMapper;

    @Test
    void shouldCreateSeatOrderAndKeepResponsesConsistent() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(BASE_DEPARTURE, 2, 2, true);

        insertPrice(route.tripId(), 1, 3, 2, 3000);

        String key = UUID.randomUUID().toString();
        MvcResult createdResult = mockMvc.perform(post(SEAT_ORDERS)
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                seatRequest(route, passenger.id(), 0, 2, 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.seatType").value(2))
                .andExpect(jsonPath("$.carriageNumber").value("01"))
                .andExpect(jsonPath("$.seatNumber").value("01A"))
                .andExpect(jsonPath("$.departureStationCode")
                        .value(route.codes().get(0)))
                .andExpect(jsonPath("$.departureStationName")
                        .value(route.names().get(0)))
                .andExpect(jsonPath("$.arrivalStationCode")
                        .value(route.codes().get(2)))
                .andExpect(jsonPath("$.arrivalStationName")
                        .value(route.names().get(2)))
                .andReturn();

        OrderCreateResponse created = read(createdResult, OrderCreateResponse.class);
        assertEquals(0, created.amount().compareTo(new BigDecimal("30.00")));

        // Both segments of the interval belong to the new order.
        assertEquals(2L, segmentCountOf(created.id()));
        assertEquals(List.of("OCCUPIED", "OCCUPIED"),
                segmentStatuses(route.seatIds().get(0)));
        assertEquals(List.of("AVAILABLE", "AVAILABLE"),
                segmentStatuses(route.seatIds().get(1)));
        assertEquals(1, quantityOfType(route, 0, 2, 2));

        // Detail and list must expose the very same snapshot.
        assertSameSnapshot(created, getOrder(session, created.id()));

        OrderPageResponse page = listOrders(session);
        OrderResponse listed = page.items().stream()
                .filter(item -> item.id().equals(created.id()))
                .findFirst()
                .orElseThrow();
        assertSameSnapshot(created, listed);

        // Payment keeps the seat occupied.
        OrderResponse paid = payOrder(session, created.id());
        assertEquals(OrderStatus.PAID, paid.status());
        assertSameSnapshot(created, paid);
        assertEquals(2L, segmentCountOf(created.id()));

        // A second seat is sold, then cancelled.
        OrderCreateResponse second = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(route, passenger.id(), 0, 2, 2), 201);
        assertEquals("02A", second.seatNumber());

        OrderResponse cancelled = cancelOrder(session, second.id());
        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        assertSameSnapshot(second, cancelled);
        assertEquals(0L, segmentCountOf(second.id()));
        assertEquals(List.of("AVAILABLE", "AVAILABLE"),
                segmentStatuses(route.seatIds().get(1)));
        assertEquals(2L, segmentCountOf(created.id()));
        assertEquals(1, quantityOfType(route, 0, 2, 2));
    }

    @Test
    void shouldReuseSeatAcrossAdjacentIntervals() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(BASE_DEPARTURE, 1, 2, true);

        insertPrice(route.tripId(), 1, 2, 2, 1000);
        insertPrice(route.tripId(), 2, 3, 2, 2000);
        insertPrice(route.tripId(), 1, 3, 2, 3000);

        OrderCreateResponse first = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(route, passenger.id(), 0, 1, 2), 201);
        OrderCreateResponse second = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(route, passenger.id(), 1, 2, 2), 201);

        // One physical seat serves both adjacent intervals.
        assertEquals("01A", first.seatNumber());
        assertEquals("01A", second.seatNumber());
        assertEquals(List.of(first.id(), second.id()),
                segmentOwners(route.seatIds().get(0)));
        assertEquals(0, quantityOfType(route, 0, 2, 2));

        // Two different orders must never be stitched into one ticket.
        assertSeatRejected(session, route.tripId(), passenger.id(),
                route.codes().get(0), route.codes().get(2), 2,
                409, "该区间席别无可用座位");

        assertEquals(2L, orderCount(route.tripId()));
        assertEquals(List.of(first.id(), second.id()),
                segmentOwners(route.seatIds().get(0)));
    }

    @Test
    void shouldRejectWhenNoSingleSeatCoversWholeInterval() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID);
        assertNotNull(userId);

        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(BASE_DEPARTURE, 2, 2, true);

        insertPrice(route.tripId(), 1, 2, 2, 1000);
        insertPrice(route.tripId(), 2, 3, 2, 2000);
        insertPrice(route.tripId(), 1, 3, 2, 3000);

        // Seat A is held on the second segment only, seat B on the first only.
        holdSeat(route, route.seatIds().get(0), 2, 3,
                userId, passenger.id(), 2);
        holdSeat(route, route.seatIds().get(1), 1, 2,
                userId, passenger.id(), 2);

        // Each segment has a free seat, but no single seat covers both.
        assertSeatRejected(session, route.tripId(), passenger.id(),
                route.codes().get(0), route.codes().get(2), 2,
                409, "该区间席别无可用座位");

        assertEquals(2L, orderCount(route.tripId()));
        assertEquals(List.of("AVAILABLE", "OCCUPIED"),
                segmentStatuses(route.seatIds().get(0)));
        assertEquals(List.of("OCCUPIED", "AVAILABLE"),
                segmentStatuses(route.seatIds().get(1)));
        assertEquals(1, quantityOfType(route, 0, 1, 2));
        assertEquals(1, quantityOfType(route, 1, 2, 2));
    }

    @Test
    void shouldRejectInvalidAndAmbiguousIntervals() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(BASE_DEPARTURE, 1, 2, true);

        insertPrice(route.tripId(), 1, 3, 2, 3000);

        String outsideCode = "Z" + uniqueSuffix();

        // Same station and reversed direction.
        assertSeatRejected(session, route.tripId(), passenger.id(),
                route.codes().get(0), route.codes().get(0), 2,
                400, "上下车站不构成该车次的有效区间");
        assertSeatRejected(session, route.tripId(), passenger.id(),
                route.codes().get(2), route.codes().get(0), 2,
                400, "上下车站不构成该车次的有效区间");

        // Unknown code and a station that is not on this run.
        assertSeatRejected(session, route.tripId(), passenger.id(),
                outsideCode, route.codes().get(2), 2,
                400, "上下车站不构成该车次的有效区间");
        assertSeatRejected(session, route.tripId(), passenger.id(),
                route.codes().get(0), outsideCode, 2,
                400, "上下车站不构成该车次的有效区间");

        assertEquals(0L, orderCount(route.tripId()));
        assertEquals(List.of("AVAILABLE", "AVAILABLE"),
                segmentStatuses(route.seatIds().get(0)));

        // The same station appears twice, so the interval is ambiguous.
        SeatRoute ambiguous = createRoute(
                BASE_DEPARTURE.plusDays(1), 1, 2, true);
        insertStop(ambiguous.tripId(), ambiguous.stationIds().get(0), 4,
                BASE_DEPARTURE.plusDays(1).plusHours(3), null);

        assertSeatRejected(session, ambiguous.tripId(), passenger.id(),
                ambiguous.codes().get(0), ambiguous.codes().get(1), 2,
                400, "该车次多次经过所购车站，暂不支持购票");

        assertEquals(0L, orderCount(ambiguous.tripId()));
        assertEquals(List.of("AVAILABLE", "AVAILABLE"),
                segmentStatuses(ambiguous.seatIds().get(0)));
    }

    @Test
    void shouldRejectIncompleteStopStructureAndMissingSegments()
            throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session);

        // Sequences 1, 2, 10, 11 leave a gap between 2 and 10.
        SeatRoute gap = createGapRoute(BASE_DEPARTURE.plusDays(2));
        insertPrice(gap.tripId(), 2, 10, 2, 1000);

        assertSeatRejected(session, gap.tripId(), passenger.id(),
                gap.codes().get(1), gap.codes().get(2), 2,
                409, "该车次经停或座位资料不完整，暂不支持区间购票");

        assertEquals(0L, orderCount(gap.tripId()));

        // A seat without the second segment row is unsellable, not free.
        SeatRoute missing = createRoute(
                BASE_DEPARTURE.plusDays(3), 1, 2, true);
        insertPrice(missing.tripId(), 1, 3, 2, 3000);

        jdbcTemplate.update("""
                DELETE FROM train_seat_segments
                WHERE seat_id = ?
                  AND segment_sequence = 2
                """, missing.seatIds().get(0));

        assertSeatRejected(session, missing.tripId(), passenger.id(),
                missing.codes().get(0), missing.codes().get(2), 2,
                409, "该车次经停或座位资料不完整，暂不支持区间购票");

        assertEquals(0L, orderCount(missing.tripId()));
        assertEquals(List.of("AVAILABLE"),
                segmentStatuses(missing.seatIds().get(0)));
    }

    @Test
    void shouldIsolatePriceAndSeatByTripIntervalAndType() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session);

        SeatRoute first = createRoute(
                BASE_DEPARTURE.plusDays(4), 1, 2, true);
        SeatRoute second = createRoute(
                BASE_DEPARTURE.plusDays(4), 1, 2, true);

        insertPrice(first.tripId(), 1, 3, 2, 3000);
        insertPrice(second.tripId(), 1, 3, 2, 4500);

        addSeat(first, 1, "01B");
        insertPrice(first.tripId(), 1, 3, 1, 8800);

        OrderCreateResponse economy = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(first, passenger.id(), 0, 2, 2), 201);
        OrderCreateResponse otherTrip = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(second, passenger.id(), 0, 2, 2), 201);
        OrderCreateResponse firstClass = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(first, passenger.id(), 0, 2, 1), 201);

        assertEquals(0, economy.amount().compareTo(new BigDecimal("30.00")));
        assertEquals(0, otherTrip.amount().compareTo(new BigDecimal("45.00")));
        assertEquals(0, firstClass.amount().compareTo(new BigDecimal("88.00")));

        assertEquals(2, economy.seatType().intValue());
        assertEquals(1, firstClass.seatType().intValue());
        assertEquals("01A", economy.seatNumber());
        assertEquals("01B", firstClass.seatNumber());
        assertFalse(economy.id().equals(otherTrip.id()));

        // A missing fare is not replaced by another interval or seat type.
        SeatRoute unconfigured = createRoute(
                BASE_DEPARTURE.plusDays(4), 1, 2, true);

        assertSeatRejected(session, unconfigured.tripId(), passenger.id(),
                unconfigured.codes().get(0), unconfigured.codes().get(2), 2,
                409, "该区间席别未配置票价");
        assertEquals(0L, orderCount(unconfigured.tripId()));
    }

    @Test
    void shouldReturnStoredOrderOnRetryWithoutNewOccupation()
            throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(
                BASE_DEPARTURE.plusDays(5), 1, 2, true);

        insertPrice(route.tripId(), 1, 3, 2, 3000);

        String key = UUID.randomUUID().toString();
        SeatOrderCreateRequest request =
                seatRequest(route, passenger.id(), 0, 2, 2);

        OrderCreateResponse created = submitSeatOrder(session, key, request, 201);
        assertEquals(2L, segmentCountOf(created.id()));

        OrderCreateResponse retried = submitSeatOrder(session, key, request, 200);

        assertEquals(created.id(), retried.id());
        assertEquals(created.amount(), retried.amount());
        assertEquals(1L, orderCount(route.tripId()));
        assertEquals(2L, segmentCountOf(created.id()));

        // Deleting the fare must not break a successful retry.
        jdbcTemplate.update("""
                DELETE FROM train_trip_interval_prices
                WHERE train_trip_id = ?
                  AND departure_sequence = 1
                  AND arrival_sequence = 3
                  AND seat_type = 2
                """, route.tripId());

        OrderCreateResponse afterPriceRemoval =
                submitSeatOrder(session, key, request, 200);

        assertEquals(created.id(), afterPriceRemoval.id());
        assertEquals(created.amount(), afterPriceRemoval.amount());
        assertEquals(2L, segmentCountOf(created.id()));

        // A new key has to read the fare again and must fail.
        assertSeatRejected(session, route.tripId(), passenger.id(),
                route.codes().get(0), route.codes().get(2), 2,
                409, "该区间席别未配置票价");
        assertEquals(1L, orderCount(route.tripId()));
    }

    @Test
    void shouldRejectSameKeyForDifferentRequests() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(
                BASE_DEPARTURE.plusDays(6), 1, 2, true);

        insertPrice(route.tripId(), 1, 3, 2, 3000);

        String key = UUID.randomUUID().toString();
        OrderCreateResponse created = submitSeatOrder(
                session, key, seatRequest(route, passenger.id(), 0, 2, 2), 201);

        // A different interval under the same key conflicts.
        mockMvc.perform(post(SEAT_ORDERS)
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                seatRequest(route, passenger.id(), 0, 1, 2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("幂等键已用于不同的下单请求"));

        // A different seat type under the same key conflicts.
        mockMvc.perform(post(SEAT_ORDERS)
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                seatRequest(route, passenger.id(), 0, 2, 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("幂等键已用于不同的下单请求"));

        // The same key cannot be reused through the priced entry either.
        mockMvc.perform(post(PRICED_ORDERS)
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PricedOrderCreateRequest(
                                        route.tripId(), passenger.id(), 2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("幂等键已用于不同的下单请求"));

        assertEquals(1L, orderCount(route.tripId()));
        assertEquals(2L, segmentCountOf(created.id()));
    }

    @Test
    void shouldRollBackOrderAndSegmentsWhenAssociationFails()
            throws Exception {
        MockHttpSession session = registerAndLogin();
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID);
        assertNotNull(userId);

        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(
                BASE_DEPARTURE.plusDays(7), 1, 2, true);

        insertPrice(route.tripId(), 1, 3, 2, 3000);

        String key = UUID.randomUUID().toString();
        SeatOrderCreateRequest request =
                seatRequest(route, passenger.id(), 0, 2, 2);

        doAnswer(invocation -> {
            // The real order and the real occupation already happened.
            assertTrue(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );

            Long orderId = jdbcTemplate.queryForObject("""
                    SELECT id
                    FROM ticket_orders
                    WHERE train_trip_id = ?
                    """, Long.class, route.tripId());

            assertNotNull(orderId);
            assertEquals(1L, orderCount(route.tripId()));
            assertEquals(2L, segmentCountOf(orderId));

            // Fail after the insert and the occupation, inside the transaction.
            return 0;
        }).doAnswer(invocation -> {
            // Later calls must run the real mapper statement.
            Map<String, Object> params = new HashMap<>();
            params.put("userId", invocation.getArgument(0));
            params.put("idempotencyKey", invocation.getArgument(1));
            params.put("orderId", invocation.getArgument(2));

            return sqlSessionTemplate.update(
                    OrderIdempotencyMapper.class.getName() + ".attachOrder",
                    params
            );
        }).when(orderIdempotencyMapper)
                .attachOrder(anyLong(), anyString(), anyLong());

        assertFalse(
                TransactionSynchronizationManager.isActualTransactionActive()
        );

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> seatOrderService.create(userId, key, request)
        );

        assertEquals(
                "Idempotency order association failed",
                failure.getMessage()
        );

        // The whole transaction must be gone: order, segments and key.
        assertEquals(0L, orderCount(route.tripId()));
        assertEquals(List.of("AVAILABLE", "AVAILABLE"),
                segmentStatuses(route.seatIds().get(0)));
        assertNull(orderIdOfSegment(route.seatIds().get(0), 1));
        assertEquals(0L, idempotencyRowCount(userId, key));

        // The same key must remain usable after the rollback.
        OrderCreateResponse recovered = submitSeatOrder(session, key, request, 201);

        assertEquals(1L, orderCount(route.tripId()));
        assertEquals(2L, segmentCountOf(recovered.id()));
        assertEquals(1L, idempotencyRowCount(userId, key));
    }

    @Test
    void shouldAllowOnlyOneOverlappingOrderUnderConcurrency()
            throws Exception {
        MockHttpSession session = registerAndLogin();
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID);
        assertNotNull(userId);

        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(
                BASE_DEPARTURE.plusDays(8), 1, 2, true);

        insertPrice(route.tripId(), 1, 3, 2, 3000);

        SeatOrderCreateRequest request =
                seatRequest(route, passenger.id(), 0, 2, 2);

        // Fixtures must be committed before worker threads read them.
        assertFalse(
                TransactionSynchronizationManager.isActualTransactionActive()
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for(int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if(!start.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Start signal timed out"
                        );
                    }

                    try {
                        // Each worker enters a separate service transaction.
                        seatOrderService.create(
                                userId, UUID.randomUUID().toString(), request
                        );
                        return true;
                    } catch(SeatNotAvailableException exception) {
                        return false;
                    }
                }));
            }

            assertTrue(ready.await(15, TimeUnit.SECONDS));
            start.countDown();

            int succeeded = 0;
            for(Future<Boolean> future : futures) {
                if(future.get(30, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }

            // One seat can only be sold once.
            assertEquals(1, succeeded);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(1L, orderCount(route.tripId()));
        assertEquals(2L, segmentCountOf(
                jdbcTemplate.queryForObject("""
                        SELECT id FROM ticket_orders WHERE train_trip_id = ?
                        """, Long.class, route.tripId())));
    }

    @Test
    void shouldCreateOneOrderForConcurrentSameKey() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID);
        assertNotNull(userId);

        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(
                BASE_DEPARTURE.plusDays(9), 1, 2, true);

        insertPrice(route.tripId(), 1, 3, 2, 3000);

        String key = UUID.randomUUID().toString();
        SeatOrderCreateRequest request =
                seatRequest(route, passenger.id(), 0, 2, 2);

        assertFalse(
                TransactionSynchronizationManager.isActualTransactionActive()
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<OrderCreateResult>> futures = new ArrayList<>();

        try {
            for(int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if(!start.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Start signal timed out"
                        );
                    }

                    return seatOrderService.create(userId, key, request);
                }));
            }

            assertTrue(ready.await(15, TimeUnit.SECONDS));
            start.countDown();

            List<Long> orderIds = new ArrayList<>();
            int createdCount = 0;

            for(Future<OrderCreateResult> future : futures) {
                OrderCreateResult result = future.get(30, TimeUnit.SECONDS);
                orderIds.add(result.order().getId());

                if(result.created()) {
                    createdCount++;
                }
            }

            // Both calls succeed, but only one of them creates the order.
            assertEquals(1, createdCount);
            assertEquals(1, orderIds.stream().distinct().count());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(1L, orderCount(route.tripId()));
        assertEquals(2L, segmentCountOf(
                jdbcTemplate.queryForObject("""
                        SELECT id FROM ticket_orders WHERE train_trip_id = ?
                        """, Long.class, route.tripId())));
        assertEquals(1L, idempotencyRowCount(userId, key));
    }

    @Test
    void shouldReleaseOnlyOwnSegmentsOnCancel() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(
                BASE_DEPARTURE.plusDays(10), 1, 2, true);

        insertPrice(route.tripId(), 1, 2, 2, 1000);
        insertPrice(route.tripId(), 2, 3, 2, 2000);
        insertPrice(route.tripId(), 1, 3, 2, 3000);

        OrderCreateResponse first = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(route, passenger.id(), 0, 1, 2), 201);
        OrderCreateResponse second = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(route, passenger.id(), 1, 2, 2), 201);

        assertEquals("01A", first.seatNumber());
        assertEquals("01A", second.seatNumber());

        cancelOrder(session, first.id());

        // The adjacent order keeps its segment.
        assertNull(orderIdOfSegment(route.seatIds().get(0), 1));
        assertEquals(second.id(), orderIdOfSegment(route.seatIds().get(0), 2));
        assertEquals(1, quantityOfType(route, 0, 1, 2));
        assertEquals(0, quantityOfType(route, 1, 2, 2));

        // A repeated cancellation must not release anything again.
        cancelOrder(session, first.id());
        assertEquals(second.id(), orderIdOfSegment(route.seatIds().get(0), 2));
        assertEquals(0L, segmentCountOf(first.id()));

        cancelOrder(session, second.id());
        assertNull(orderIdOfSegment(route.seatIds().get(0), 2));
        assertEquals(1, quantityOfType(route, 0, 2, 2));
    }

    @Test
    void shouldReleaseOnceOnTimeoutAndKeepPaidOrder() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(
                BASE_DEPARTURE.plusDays(11), 1, 2, true);

        insertPrice(route.tripId(), 1, 3, 2, 3000);

        OrderCreateResponse expired = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(route, passenger.id(), 0, 2, 2), 201);

        moveDeadlineToPast(expired.id());

        assertTrue(ticketOrderService.closeExpired(expired.id()));
        assertEquals("EXPIRED", orderStatus(expired.id()));
        assertEquals(List.of("AVAILABLE", "AVAILABLE"),
                segmentStatuses(route.seatIds().get(0)));

        // Only the caller that changed the status may release.
        assertFalse(ticketOrderService.closeExpired(expired.id()));
        assertEquals(0L, segmentCountOf(expired.id()));

        mockMvc.perform(post("/api/orders/{id}/pay", expired.id())
                        .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("订单已超时关闭，无法支付"));

        // A paid order keeps the seat and cannot be closed or cancelled.
        OrderCreateResponse paid = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(route, passenger.id(), 0, 2, 2), 201);
        payOrder(session, paid.id());

        assertFalse(ticketOrderService.closeExpired(paid.id()));
        assertEquals(2L, segmentCountOf(paid.id()));

        mockMvc.perform(post("/api/orders/{id}/cancel", paid.id())
                        .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("当前订单状态不允许取消"));

        assertEquals(2L, segmentCountOf(paid.id()));
    }

    @Test
    void shouldCloseSeatOrderOnceUnderConcurrentClosing() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(
                BASE_DEPARTURE.plusDays(14), 1, 2, true);

        insertPrice(route.tripId(), 1, 3, 2, 3000);

        OrderCreateResponse order = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(route, passenger.id(), 0, 2, 2), 201);
        moveDeadlineToPast(order.id());

        assertFalse(
                TransactionSynchronizationManager.isActualTransactionActive()
        );

        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for(int i = 0; i < 4; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if(!start.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Start signal timed out"
                        );
                    }

                    return ticketOrderService.closeExpired(order.id());
                }));
            }

            assertTrue(ready.await(15, TimeUnit.SECONDS));
            start.countDown();

            int closed = 0;
            for(Future<Boolean> future : futures) {
                if(future.get(30, TimeUnit.SECONDS)) {
                    closed++;
                }
            }

            // Only the status transition winner may release segments.
            assertEquals(1, closed);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals("EXPIRED", orderStatus(order.id()));
        assertEquals(List.of("AVAILABLE", "AVAILABLE"),
                segmentStatuses(route.seatIds().get(0)));
        assertEquals(0L, segmentCountOf(order.id()));
        assertEquals(1, quantityOfType(route, 0, 2, 2));
    }

    @Test
    void shouldKeepCancellationAndClosingConsistent() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID);
        assertNotNull(userId);

        PassengerResponse passenger = createPassenger(session);
        SeatRoute route = createRoute(
                BASE_DEPARTURE.plusDays(15), 1, 2, true);

        insertPrice(route.tripId(), 1, 3, 2, 3000);

        OrderCreateResponse order = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(route, passenger.id(), 0, 2, 2), 201);
        moveDeadlineToPast(order.id());

        assertFalse(
                TransactionSynchronizationManager.isActualTransactionActive()
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> cancelling = executor.submit(() -> {
                ready.countDown();

                if(!start.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Start signal timed out"
                    );
                }

                try {
                    ticketOrderService.cancel(userId, order.id());
                    return true;
                } catch(OrderStateConflictException exception) {
                    return false;
                }
            });

            Future<Boolean> closing = executor.submit(() -> {
                ready.countDown();

                if(!start.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Start signal timed out"
                    );
                }

                return ticketOrderService.closeExpired(order.id());
            });

            assertTrue(ready.await(15, TimeUnit.SECONDS));
            start.countDown();

            boolean cancelled = cancelling.get(30, TimeUnit.SECONDS);
            boolean closed = closing.get(30, TimeUnit.SECONDS);

            // Exactly one of the two transitions may win.
            assertTrue(cancelled ^ closed);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        String status = orderStatus(order.id());
        assertTrue(status.equals("CANCELLED") || status.equals("EXPIRED"));
        assertEquals(0L, segmentCountOf(order.id()));
        assertEquals(List.of("AVAILABLE", "AVAILABLE"),
                segmentStatuses(route.seatIds().get(0)));
        assertEquals(1, quantityOfType(route, 0, 2, 2));
    }

    @Test
    void shouldKeepQuantityAndSeatSalesExclusive() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session);

        // A prepared quantity instance keeps its old behaviour.
        SeatRoute route = createRoute(
                BASE_DEPARTURE.plusDays(12), 1, 2, false);
        insertPrice(route.tripId(), 1, 3, 2, 3000);
        createInventory(route.tripId(), 5, 5);

        assertSeatRejected(session, route.tripId(), passenger.id(),
                route.codes().get(0), route.codes().get(2), 2,
                409, "该车次尚未启用座位售票");

        String legacyKey = UUID.randomUUID().toString();
        OrderCreateResponse legacy = submitLegacyOrder(
                session, legacyKey, route.tripId(), passenger.id(), 201);

        assertEquals(4, remainingTickets(route.tripId()));

        // The switch must refuse while a pending order still exists.
        assertEquals(0, enableSeatSales(route.tripId()));
        assertEquals("QUANTITY", salesMode(route.tripId()));

        cancelOrder(session, legacy.id());
        assertEquals(5, remainingTickets(route.tripId()));

        // With no active order the guarded switch takes effect.
        assertEquals(1, enableSeatSales(route.tripId()));
        assertEquals("SEAT", salesMode(route.tripId()));

        // The old entry is closed on this run, the seat entry opens.
        mockMvc.perform(post(LEGACY_ORDERS)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderCreateRequest(
                                        route.tripId(), passenger.id()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("该车次已启用座位售票，请使用座位购票入口"));

        OrderCreateResponse seatOrder = submitSeatOrder(
                session, UUID.randomUUID().toString(),
                seatRequest(route, passenger.id(), 0, 2, 2), 201);

        assertEquals(2L, segmentCountOf(seatOrder.id()));

        // Seat sales never touch the old quantity counter.
        assertEquals(5, remainingTickets(route.tripId()));
    }

    @Test
    void shouldProtectSeatOrderEndpoints() throws Exception {
        SeatRoute route = createRoute(
                BASE_DEPARTURE.plusDays(13), 1, 2, true);
        insertPrice(route.tripId(), 1, 3, 2, 3000);

        // No login means no purchase and no ownership.
        mockMvc.perform(post(SEAT_ORDERS)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SeatOrderCreateRequest(
                                        route.tripId(), 1L,
                                        route.codes().get(0),
                                        route.codes().get(2), 2))))
                .andExpect(status().isUnauthorized());

        MockHttpSession ownerSession = registerAndLogin();
        PassengerResponse ownerPassenger = createPassenger(ownerSession);

        OrderCreateResponse order = submitSeatOrder(
                ownerSession, UUID.randomUUID().toString(),
                seatRequest(route, ownerPassenger.id(), 0, 2, 2), 201);

        // Another account cannot use the passenger or read the order.
        MockHttpSession otherSession = registerAndLogin();

        mockMvc.perform(post(SEAT_ORDERS)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .session(otherSession)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                seatRequest(route, ownerPassenger.id(), 0, 2, 2))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("乘车人不存在"));

        mockMvc.perform(get("/api/orders/{id}", order.id())
                        .session(otherSession))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/orders/{id}/pay", order.id())
                        .session(otherSession))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/orders/{id}/cancel", order.id())
                        .session(otherSession))
                .andExpect(status().isNotFound());

        // Request validation still applies.
        mockMvc.perform(post(SEAT_ORDERS)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .session(ownerSession)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                seatRequest(route, ownerPassenger.id(), 0, 2, 3))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(SEAT_ORDERS)
                        .session(ownerSession)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                seatRequest(route, ownerPassenger.id(), 0, 2, 2))))
                .andExpect(status().isBadRequest());

        assertEquals(1L, orderCount(route.tripId()));
    }

    @Test
    void shouldConstrainSeatOrderStructure() {
        List<String> nullableSeatColumns = jdbcTemplate.queryForList("""
                SELECT COLUMN_NAME
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'ticket_orders'
                  AND COLUMN_NAME IN (
                      'carriage_number', 'seat_number', 'seat_id'
                  )
                  AND IS_NULLABLE = 'YES'
                ORDER BY COLUMN_NAME
                """, String.class);

        assertEquals(
                List.of("carriage_number", "seat_id", "seat_number"),
                nullableSeatColumns
        );

        List<String> orderSeatForeignKey = jdbcTemplate.queryForList("""
                SELECT CONCAT(COLUMN_NAME, '->', REFERENCED_COLUMN_NAME)
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND CONSTRAINT_NAME = 'fk_ticket_orders_seat'
                ORDER BY ORDINAL_POSITION
                """, String.class);

        assertEquals(
                List.of("train_trip_id->train_trip_id", "seat_id->id"),
                orderSeatForeignKey
        );

        String seatTable = jdbcTemplate.queryForObject("""
                SELECT REFERENCED_TABLE_NAME
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND CONSTRAINT_NAME = 'fk_ticket_orders_seat'
                LIMIT 1
                """, String.class);

        assertEquals("train_seats", seatTable);

        List<String> seatIndex = jdbcTemplate.queryForList("""
                SELECT COLUMN_NAME
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'ticket_orders'
                  AND INDEX_NAME = 'idx_ticket_orders_seat'
                ORDER BY SEQ_IN_INDEX
                """, String.class);

        assertEquals(List.of("train_trip_id", "seat_id"), seatIndex);

        List<String> segmentOwnerForeignKey = jdbcTemplate.queryForList("""
                SELECT CONCAT(COLUMN_NAME, '->', REFERENCED_COLUMN_NAME)
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND CONSTRAINT_NAME = 'fk_train_seat_segments_order'
                ORDER BY ORDINAL_POSITION
                """, String.class);

        assertEquals(List.of("order_id->id"), segmentOwnerForeignKey);

        String salesModeNullable = jdbcTemplate.queryForObject("""
                SELECT IS_NULLABLE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'train_trips'
                  AND COLUMN_NAME = 'sales_mode'
                """, String.class);

        assertEquals("NO", salesModeNullable);

        // Orders created before V9 must not pretend to own a seat.
        Long legacySeatRows = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ticket_orders
                WHERE purchase_mode <> 'SEAT'
                  AND (
                      carriage_number IS NOT NULL
                      OR seat_number IS NOT NULL
                      OR seat_id IS NOT NULL
                  )
                """, Long.class);

        assertNotNull(legacySeatRows);
        assertEquals(0L, legacySeatRows.longValue());
    }

    private record SeatRoute(
            Long tripId,
            LocalDateTime departure,
            List<Long> stationIds,
            List<String> codes,
            List<String> names,
            List<Long> seatIds
    ) {
    }

    private record StopPoint(
            int sequence,
            String code,
            String name,
            LocalDateTime arrivalAt,
            LocalDateTime departureAt
    ) {
    }

    private SeatRoute createRoute(
            LocalDateTime departure,
            int seatCount,
            int seatType,
            boolean seatSales
    ) {
        String suffix = uniqueSuffix();
        List<String> codes = List.of(
                "A" + suffix, "B" + suffix, "C" + suffix);
        List<String> names = List.of(
                "甲站" + suffix, "乙站" + suffix, "丙站" + suffix);

        List<Long> stationIds = new ArrayList<>();
        for(int i = 0; i < codes.size(); i++) {
            stationIds.add(insertStation(codes.get(i), names.get(i)));
        }

        Long tripId = insertTrip(
                "T" + suffix, names.get(0), names.get(2),
                departure, departure.plusHours(2)
        );

        insertStop(tripId, stationIds.get(0), 1, null, departure);
        insertStop(tripId, stationIds.get(1), 2,
                departure.plusHours(1),
                departure.plusHours(1).plusMinutes(5));
        insertStop(tripId, stationIds.get(2), 3,
                departure.plusHours(2), null);

        Long carriageId = insertCarriage(tripId, "01");
        List<Long> seatIds = new ArrayList<>();

        for(int i = 0; i < seatCount; i++) {
            String seatNumber = "0" + (i + 1) + "A";
            seatIds.add(insertSeatWithSegments(
                    tripId, carriageId, seatNumber, seatType));
        }

        if(seatSales) {
            assertEquals(1, enableSeatSales(tripId));
        }

        return new SeatRoute(
                tripId, departure, stationIds, codes, names, seatIds);
    }

    private SeatRoute createGapRoute(LocalDateTime departure) {
        String suffix = uniqueSuffix();
        List<String> codes = List.of(
                "A" + suffix, "B" + suffix,
                "C" + suffix, "D" + suffix);
        List<String> names = List.of(
                "甲站" + suffix, "乙站" + suffix,
                "丙站" + suffix, "丁站" + suffix);

        List<Long> stationIds = new ArrayList<>();
        for(int i = 0; i < codes.size(); i++) {
            stationIds.add(insertStation(codes.get(i), names.get(i)));
        }

        Long tripId = insertTrip(
                "T" + suffix, names.get(0), names.get(3),
                departure, departure.plusHours(3)
        );

        // Sequences 1, 2, 10, 11 leave a gap between 2 and 10.
        insertStop(tripId, stationIds.get(0), 1, null, departure);
        insertStop(tripId, stationIds.get(1), 2,
                departure.plusHours(1),
                departure.plusHours(1).plusMinutes(5));
        insertStop(tripId, stationIds.get(2), 10,
                departure.plusHours(2),
                departure.plusHours(2).plusMinutes(5));
        insertStop(tripId, stationIds.get(3), 11,
                departure.plusHours(3), null);

        Long carriageId = insertCarriage(tripId, "01");
        Long seatId = insertSeat(tripId, carriageId, "01A", 2);
        insertSegment(tripId, seatId, 1);
        insertSegment(tripId, seatId, 2);
        insertSegment(tripId, seatId, 10);

        assertEquals(1, enableSeatSales(tripId));

        return new SeatRoute(
                tripId, departure, stationIds, codes, names, List.of(seatId));
    }

    private Long addSeat(SeatRoute route, int seatType, String seatNumber) {
        Long carriageId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM train_carriages
                WHERE train_trip_id = ?
                  AND carriage_number = '01'
                """, Long.class, route.tripId());

        Long seatId = insertSeat(
                route.tripId(), carriageId, seatNumber, seatType);

        insertSegment(route.tripId(), seatId, 1);
        insertSegment(route.tripId(), seatId, 2);

        return seatId;
    }

    private Long insertSeatWithSegments(
            Long trainTripId,
            Long carriageId,
            String seatNumber,
            int seatType
    ) {
        Long seatId = insertSeat(
                trainTripId, carriageId, seatNumber, seatType);

        insertSegment(trainTripId, seatId, 1);
        insertSegment(trainTripId, seatId, 2);

        return seatId;
    }

    private Long holdSeat(
            SeatRoute route,
            Long seatId,
            int fromSequence,
            int toSequence,
            Long userId,
            Long passengerId,
            int seatType
    ) {
        StopPoint boarding = stopPoint(route.tripId(), fromSequence);
        StopPoint alighting = stopPoint(route.tripId(), toSequence);

        Map<String, Object> seat = jdbcTemplate.queryForMap("""
                SELECT s.seat_number, c.carriage_number
                FROM train_seats s
                JOIN train_carriages c
                  ON c.id = s.carriage_id
                 AND c.train_trip_id = s.train_trip_id
                WHERE s.id = ?
                """, seatId);

        jdbcTemplate.update("""
                INSERT INTO ticket_orders (
                    user_id, train_trip_id, passenger_id,
                    passenger_name, passenger_id_card,
                    status, pay_deadline, purchase_mode,
                    departure_sequence, arrival_sequence,
                    departure_station_code, departure_station_name,
                    departure_at,
                    arrival_station_code, arrival_station_name,
                    arrival_at,
                    seat_type, amount_cents, seat_id,
                    carriage_number, seat_number
                )
                VALUES (
                    ?, ?, ?, ?, ?,
                    'PAID', DATE_ADD(NOW(), INTERVAL 30 MINUTE), 'SEAT',
                    ?, ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?, ?
                )
                """,
                userId, route.tripId(), passengerId,
                "Held Rider", "000000000000000000",
                boarding.sequence(), alighting.sequence(),
                boarding.code(), boarding.name(), boarding.departureAt(),
                alighting.code(), alighting.name(), alighting.arrivalAt(),
                seatType, 1000, seatId,
                seat.get("carriage_number"), seat.get("seat_number"));

        Long orderId = jdbcTemplate.queryForObject("""
                SELECT MAX(id)
                FROM ticket_orders
                WHERE train_trip_id = ?
                """, Long.class, route.tripId());

        assertNotNull(orderId);

        int occupied = jdbcTemplate.update("""
                UPDATE train_seat_segments
                SET seat_status = 'OCCUPIED',
                    order_id = ?
                WHERE train_trip_id = ?
                  AND seat_id = ?
                  AND segment_sequence >= ?
                  AND segment_sequence < ?
                """, orderId, route.tripId(), seatId,
                fromSequence, toSequence);

        assertEquals(toSequence - fromSequence, occupied);

        return orderId;
    }

    private StopPoint stopPoint(Long tripId, int sequence) {
        return jdbcTemplate.queryForObject("""
                SELECT s.stop_sequence AS sequence,
                       st.code AS code,
                       st.name AS name,
                       s.arrival_at AS arrivalAt,
                       s.departure_at AS departureAt
                FROM train_trip_stops s
                JOIN stations st ON st.id = s.station_id
                WHERE s.train_trip_id = ?
                  AND s.stop_sequence = ?
                """, (rs, rowNum) -> new StopPoint(
                rs.getInt("sequence"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getObject("arrivalAt", LocalDateTime.class),
                rs.getObject("departureAt", LocalDateTime.class)
        ), tripId, sequence);
    }

    private SeatOrderCreateRequest seatRequest(
            SeatRoute route,
            Long passengerId,
            int fromIndex,
            int toIndex,
            int seatType
    ) {
        return new SeatOrderCreateRequest(
                route.tripId(),
                passengerId,
                route.codes().get(fromIndex),
                route.codes().get(toIndex),
                seatType
        );
    }

    private OrderCreateResponse submitSeatOrder(
            MockHttpSession session,
            String key,
            SeatOrderCreateRequest request,
            int expectedStatus
    ) throws Exception {
        MvcResult result = mockMvc.perform(post(SEAT_ORDERS)
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus))
                .andReturn();

        return read(result, OrderCreateResponse.class);
    }

    private OrderCreateResponse submitLegacyOrder(
            MockHttpSession session,
            String key,
            Long tripId,
            Long passengerId,
            int expectedStatus
    ) throws Exception {
        MvcResult result = mockMvc.perform(post(LEGACY_ORDERS)
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderCreateRequest(tripId, passengerId))))
                .andExpect(status().is(expectedStatus))
                .andReturn();

        return read(result, OrderCreateResponse.class);
    }

    private void assertSeatRejected(
            MockHttpSession session,
            Long tripId,
            Long passengerId,
            String fromStation,
            String toStation,
            int seatType,
            int expectedStatus,
            String expectedMessage
    ) throws Exception {
        mockMvc.perform(post(SEAT_ORDERS)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SeatOrderCreateRequest(
                                        tripId, passengerId,
                                        fromStation, toStation, seatType))))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private OrderResponse getOrder(MockHttpSession session, Long id)
            throws Exception {
        MvcResult result = mockMvc.perform(get("/api/orders/{id}", id)
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, OrderResponse.class);
    }

    private OrderPageResponse listOrders(MockHttpSession session)
            throws Exception {
        MvcResult result = mockMvc.perform(get("/api/orders")
                        .param("page", "1")
                        .param("size", "100")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, OrderPageResponse.class);
    }

    private OrderResponse payOrder(MockHttpSession session, Long id)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders/{id}/pay", id)
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, OrderResponse.class);
    }

    private OrderResponse cancelOrder(MockHttpSession session, Long id)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders/{id}/cancel", id)
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        return read(result, OrderResponse.class);
    }

    private void assertSameSnapshot(
            OrderCreateResponse expected,
            OrderResponse actual
    ) {
        assertEquals(expected.id(), actual.id());
        assertEquals(expected.trainTripId(), actual.trainTripId());
        assertEquals(expected.passengerId(), actual.passengerId());
        assertEquals(expected.departureStationCode(),
                actual.departureStationCode());
        assertEquals(expected.departureStationName(),
                actual.departureStationName());
        assertEquals(expected.departureAt(), actual.departureAt());
        assertEquals(expected.arrivalStationCode(),
                actual.arrivalStationCode());
        assertEquals(expected.arrivalStationName(),
                actual.arrivalStationName());
        assertEquals(expected.arrivalAt(), actual.arrivalAt());
        assertEquals(expected.seatType(), actual.seatType());
        assertEquals(expected.carriageNumber(), actual.carriageNumber());
        assertEquals(expected.seatNumber(), actual.seatNumber());
        assertEquals(0, expected.amount().compareTo(actual.amount()));
    }

    private int quantityOfType(
            SeatRoute route,
            int fromIndex,
            int toIndex,
            int seatType
    ) throws Exception {
        MvcResult result = mockMvc.perform(get(TICKET_QUERY)
                        .param("departureDate",
                                route.departure().toLocalDate().toString())
                        .param("fromStation", route.codes().get(fromIndex))
                        .param("toStation", route.codes().get(toIndex)))
                .andExpect(status().isOk())
                .andReturn();

        TicketPageQueryResponse response =
                read(result, TicketPageQueryResponse.class);

        assertEquals(1, response.trainList().size());

        return response.trainList().get(0).seatClassList().stream()
                .filter(seat -> seat.type() == seatType)
                .map(SeatClassResponse::quantity)
                .findFirst()
                .orElseThrow();
    }

    private List<String> segmentStatuses(Long seatId) {
        return jdbcTemplate.queryForList("""
                SELECT seat_status
                FROM train_seat_segments
                WHERE seat_id = ?
                ORDER BY segment_sequence
                """, String.class, seatId);
    }

    private List<Long> segmentOwners(Long seatId) {
        return jdbcTemplate.queryForList("""
                SELECT order_id
                FROM train_seat_segments
                WHERE seat_id = ?
                ORDER BY segment_sequence
                """, Long.class, seatId);
    }

    private Long orderIdOfSegment(Long seatId, int segmentSequence) {
        return jdbcTemplate.queryForObject("""
                SELECT order_id
                FROM train_seat_segments
                WHERE seat_id = ?
                  AND segment_sequence = ?
                """, Long.class, seatId, segmentSequence);
    }

    private long segmentCountOf(Long orderId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM train_seat_segments
                WHERE order_id = ?
                """, Long.class, orderId);

        assertNotNull(count);
        return count;
    }

    private long orderCount(Long tripId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ticket_orders
                WHERE train_trip_id = ?
                """, Long.class, tripId);

        assertNotNull(count);
        return count;
    }

    private long idempotencyRowCount(Long userId, String key) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM order_idempotency
                WHERE user_id = ?
                  AND idempotency_key = ?
                """, Long.class, userId, key);

        assertNotNull(count);
        return count;
    }

    private String orderStatus(Long orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM ticket_orders WHERE id = ?",
                String.class,
                orderId
        );
    }

    private int remainingTickets(Long tripId) {
        Integer remaining = jdbcTemplate.queryForObject("""
                SELECT remaining_tickets
                FROM ticket_inventory
                WHERE train_trip_id = ?
                """, Integer.class, tripId);

        assertNotNull(remaining);
        return remaining;
    }

    private String salesMode(Long tripId) {
        return jdbcTemplate.queryForObject(
                "SELECT sales_mode FROM train_trips WHERE id = ?",
                String.class,
                tripId
        );
    }

    // The only supported way to switch a run, guarded by active orders.
    private int enableSeatSales(Long tripId) {
        return jdbcTemplate.update("""
                UPDATE train_trips
                SET sales_mode = 'SEAT'
                WHERE id = ?
                  AND sales_mode = 'QUANTITY'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM ticket_orders o
                      WHERE o.train_trip_id = ?
                        AND o.status IN ('PENDING_PAYMENT', 'PAID')
                  )
                """, tripId, tripId);
    }

    private void moveDeadlineToPast(Long orderId) {
        jdbcTemplate.update("""
                UPDATE ticket_orders
                SET pay_deadline = DATE_SUB(NOW(), INTERVAL 1 MINUTE)
                WHERE id = ?
                """, orderId);
    }

    private <T> T read(MvcResult result, Class<T> type) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsByteArray(), type);
    }

    private MockHttpSession registerAndLogin() throws Exception {
        String username = "seat_" + UUID.randomUUID()
                .toString().replace("-", "");
        String credentials = objectMapper.writeValueAsString(
                new Credentials(username, "Train123"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/users/login")
                        .contentType(APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    private PassengerResponse createPassenger(MockHttpSession session)
            throws Exception {
        mockMvc.perform(post("/api/passengers")
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PassengerSaveRequest(
                                        "Seat Rider",
                                        "000000000000000000",
                                        "13800000000"))))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/passengers")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        PassengerResponse[] passengers = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                PassengerResponse[].class
        );

        assertTrue(passengers.length > 0);
        return passengers[passengers.length - 1];
    }

    private Long insertStation(String code, String name) {
        jdbcTemplate.update("""
                INSERT INTO stations (code, name, spell, region_name)
                VALUES (?, ?, ?, ?)
                """, code, name, "test", "测试地区");

        return jdbcTemplate.queryForObject(
                "SELECT id FROM stations WHERE code = ?", Long.class, code);
    }

    private Long insertTrip(
            String trainNumber,
            String fromStation,
            String toStation,
            LocalDateTime departureAt,
            LocalDateTime arrivalAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO train_trips (
                    train_number, departure_station, arrival_station,
                    departure_at, arrival_at
                )
                VALUES (?, ?, ?, ?, ?)
                """, trainNumber, fromStation, toStation,
                departureAt, arrivalAt);

        return jdbcTemplate.queryForObject("""
                SELECT id FROM train_trips
                WHERE train_number = ?
                  AND departure_at = ?
                """, Long.class, trainNumber, departureAt);
    }

    private void insertStop(
            Long trainTripId,
            Long stationId,
            int stopSequence,
            LocalDateTime arrivalAt,
            LocalDateTime departureAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO train_trip_stops (
                    train_trip_id, station_id, stop_sequence,
                    arrival_at, departure_at
                )
                VALUES (?, ?, ?, ?, ?)
                """, trainTripId, stationId, stopSequence,
                arrivalAt, departureAt);
    }

    private Long insertCarriage(Long trainTripId, String carriageNumber) {
        jdbcTemplate.update("""
                INSERT INTO train_carriages (train_trip_id, carriage_number)
                VALUES (?, ?)
                """, trainTripId, carriageNumber);

        return jdbcTemplate.queryForObject("""
                SELECT id FROM train_carriages
                WHERE train_trip_id = ?
                  AND carriage_number = ?
                """, Long.class, trainTripId, carriageNumber);
    }

    private Long insertSeat(
            Long trainTripId,
            Long carriageId,
            String seatNumber,
            int seatType
    ) {
        jdbcTemplate.update("""
                INSERT INTO train_seats (
                    train_trip_id, carriage_id, seat_number, seat_type
                )
                VALUES (?, ?, ?, ?)
                """, trainTripId, carriageId, seatNumber, seatType);

        return jdbcTemplate.queryForObject("""
                SELECT id FROM train_seats
                WHERE train_trip_id = ?
                  AND carriage_id = ?
                  AND seat_number = ?
                """, Long.class, trainTripId, carriageId, seatNumber);
    }

    private void insertSegment(
            Long trainTripId,
            Long seatId,
            int segmentSequence
    ) {
        jdbcTemplate.update("""
                INSERT INTO train_seat_segments (
                    train_trip_id, seat_id, segment_sequence, seat_status
                )
                VALUES (?, ?, ?, 'AVAILABLE')
                """, trainTripId, seatId, segmentSequence);
    }

    private void insertPrice(
            Long trainTripId,
            int departureSequence,
            int arrivalSequence,
            int seatType,
            int priceCents
    ) {
        jdbcTemplate.update("""
                INSERT INTO train_trip_interval_prices (
                    train_trip_id, departure_sequence, arrival_sequence,
                    seat_type, price_cents
                )
                VALUES (?, ?, ?, ?, ?)
                """, trainTripId, departureSequence, arrivalSequence,
                seatType, priceCents);
    }

    private void createInventory(
            Long trainTripId,
            int totalTickets,
            int remainingTickets
    ) {
        jdbcTemplate.update("""
                INSERT INTO ticket_inventory (
                    train_trip_id, total_tickets, remaining_tickets
                )
                VALUES (?, ?, ?)
                """, trainTripId, totalTickets, remainingTickets);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase(Locale.ROOT);
    }

    private record Credentials(String username, String password) {
    }
}
