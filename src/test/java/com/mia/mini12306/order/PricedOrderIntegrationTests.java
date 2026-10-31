package com.mia.mini12306.order;

import com.mia.mini12306.order.dto.OrderCreateRequest;
import com.mia.mini12306.order.dto.OrderCreateResponse;
import com.mia.mini12306.order.mapper.TicketOrderMapper;
import com.mia.mini12306.passenger.dto.PassengerResponse;
import com.mia.mini12306.passenger.dto.PassengerSaveRequest;
import com.mia.mini12306.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class PricedOrderIntegrationTests extends DatabaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TicketOrderService ticketOrderService;

    @MockitoSpyBean
    private TicketOrderMapper ticketOrderMapper;

    @Test
    void shouldPriceFullRouteAndIgnoreClientAmount() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session, randomPassenger());
        Route route = createRoute(5);
        createPrice(route.tripId(), 1, 3, 2, 12345);

        // The extra amount must not influence the stored price.
        String body = """
                {"trainTripId": %d, "passengerId": %d, "seatType": 2, "amount": 1}
                """.formatted(route.tripId(), passenger.id());

        MvcResult result = mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatType").value(2))
                .andExpect(jsonPath("$.amount").value(123.45))
                .andReturn();

        OrderCreateResponse created = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                OrderCreateResponse.class
        );

        assertEquals(0, new BigDecimal("123.45")
                .compareTo(created.amount()));

        // Read the stored snapshot instead of trusting the response.
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT purchase_mode, departure_sequence, arrival_sequence,
                       departure_station_code, departure_station_name,
                       departure_at, arrival_station_code, arrival_station_name,
                       arrival_at, seat_type, amount_cents
                FROM ticket_orders
                WHERE id = ?
                """, created.id());

        assertEquals("PRICED", row.get("purchase_mode"));
        assertEquals(1,
                ((Number) row.get("departure_sequence")).intValue());
        assertEquals(3,
                ((Number) row.get("arrival_sequence")).intValue());
        assertEquals(route.firstStationCode(),
                row.get("departure_station_code"));
        assertEquals(route.firstName(),
                row.get("departure_station_name"));
        assertEquals(route.lastStationCode(),
                row.get("arrival_station_code"));
        assertEquals(route.lastName(),
                row.get("arrival_station_name"));
        assertEquals(2, ((Number) row.get("seat_type")).intValue());
        assertEquals(12345, ((Number) row.get("amount_cents")).intValue());
        assertEquals(route.departureAt(), row.get("departure_at"));
        assertEquals(route.arrivalAt(), row.get("arrival_at"));

        assertEquals(4, remainingTickets(route.tripId()));
    }

    @Test
    void shouldExposeSnapshotOnDetailListAndPayment() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session, randomPassenger());
        Route route = createRoute(5);
        createPrice(route.tripId(), 1, 3, 2, 12345);

        OrderCreateResponse created = createPricedOrder(
                session, route.tripId(), passenger.id(), 2
        );

        mockMvc.perform(get("/api/orders/{id}", created.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatType").value(2))
                .andExpect(jsonPath("$.amount").value(123.45));

        mockMvc.perform(get("/api/orders")
                        .param("page", "1")
                        .param("size", "10")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(created.id()))
                .andExpect(jsonPath("$.items[0].seatType").value(2))
                .andExpect(jsonPath("$.items[0].amount").value(123.45));

        mockMvc.perform(post("/api/orders/{id}/pay", created.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.seatType").value(2))
                .andExpect(jsonPath("$.amount").value(123.45));

        // Payment must not rewrite the snapshot.
        assertEquals(12345, amountCentsOf(created.id()));
        assertEquals(4, remainingTickets(route.tripId()));
    }

    @Test
    void shouldNotMixPartialOtherTripOrOtherSeatPrices() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session, randomPassenger());

        // Only partial intervals are priced; the full route is not.
        Route partial = createRoute(5);
        createPrice(partial.tripId(), 1, 2, 2, 100);
        createPrice(partial.tripId(), 2, 3, 2, 200);

        String partialKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", partialKey)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(pricedBody(partial.tripId(), passenger.id(), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("该车次全程席别未配置票价"));

        assertEquals(5, remainingTickets(partial.tripId()));
        assertEquals(0, orderCount(partial.tripId()));
        assertEquals(0, idempotencyCount(partialKey));

        // Another seat type is priced, but not the requested one.
        Route otherSeat = createRoute(5);
        createPrice(otherSeat.tripId(), 1, 3, 0, 777);

        mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(pricedBody(otherSeat.tripId(), passenger.id(), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("该车次全程席别未配置票价"));

        assertEquals(5, remainingTickets(otherSeat.tripId()));

        // Two trips share the same stop range and seat type, but differ in price.
        Route priced = createRoute(5);
        createPrice(priced.tripId(), 1, 3, 2, 555);

        MvcResult pricedResult = mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(pricedBody(priced.tripId(), passenger.id(), 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(5.55))
                .andReturn();

        OrderCreateResponse pricedOrder = objectMapper.readValue(
                pricedResult.getResponse().getContentAsByteArray(),
                OrderCreateResponse.class
        );

        Route competitor = createRoute(5);
        createPrice(competitor.tripId(), 1, 3, 2, 777);

        MvcResult competitorResult = mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(pricedBody(competitor.tripId(), passenger.id(), 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(7.77))
                .andReturn();

        OrderCreateResponse competitorOrder = objectMapper.readValue(
                competitorResult.getResponse().getContentAsByteArray(),
                OrderCreateResponse.class
        );

        // Each order keeps the price of its own trip.
        assertEquals(555, amountCentsOf(pricedOrder.id()));
        assertEquals(777, amountCentsOf(competitorOrder.id()));
        assertEquals(4, remainingTickets(priced.tripId()));
        assertEquals(4, remainingTickets(competitor.tripId()));

        // A third trip has no full-route price even though the others do.
        Route unpriced = createRoute(5);
        String unpricedKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", unpricedKey)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(pricedBody(unpriced.tripId(), passenger.id(), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("该车次全程席别未配置票价"));

        assertEquals(5, remainingTickets(unpriced.tripId()));
        assertEquals(0, orderCount(unpriced.tripId()));
        assertEquals(0, idempotencyCount(unpricedKey));
    }

    @Test
    void shouldRejectBrokenStopsWithoutSideEffects() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session, randomPassenger());

        // No stops at all.
        Long emptyTrip = createBareTrip("空经停起点", "空经停终点");
        assertStopsRejected(session, passenger.id(), emptyTrip, 5);

        // A single stop cannot describe a route.
        Long singleTrip = createBareTrip("单站起点", "单站终点");
        String singleCode = "U" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        insertStation(singleCode, "单站");
        insertStop(singleTrip, singleCode, 1, null,
                LocalDateTime.of(2099, 6, 1, 8, 0));
        assertStopsRejected(session, passenger.id(), singleTrip, 5);

        // Stops no longer match the endpoints advertised by the trip.
        Route broken = createRoute(5);
        jdbcTemplate.update("""
                UPDATE train_trips SET departure_station = ? WHERE id = ?
                """, "被改过的起点", broken.tripId());
        assertStopsRejected(session, passenger.id(), broken.tripId(), 5);
    }

    @Test
    void shouldReplayOriginalAmountAfterPriceChanges() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session, randomPassenger());
        Route route = createRoute(5);
        createPrice(route.tripId(), 1, 3, 2, 12345);

        String key = UUID.randomUUID().toString();
        String body = pricedBody(route.tripId(), passenger.id(), 2);

        MvcResult first = mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        OrderCreateResponse created = objectMapper.readValue(
                first.getResponse().getContentAsByteArray(),
                OrderCreateResponse.class
        );

        // Reprice the interval; the stored order must not follow it.
        jdbcTemplate.update("""
                UPDATE train_trip_interval_prices
                SET price_cents = 99999
                WHERE train_trip_id = ?
                  AND departure_sequence = 1
                  AND arrival_sequence = 3
                  AND seat_type = 2
                """, route.tripId());

        mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.amount").value(123.45));

        jdbcTemplate.update("""
                DELETE FROM train_trip_interval_prices
                WHERE train_trip_id = ?
                """, route.tripId());

        mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(123.45));

        assertEquals(12345, amountCentsOf(created.id()));
        assertEquals(4, remainingTickets(route.tripId()));
    }

    @Test
    void shouldRejectSameKeyAcrossSeatTypeOrEntry() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session, randomPassenger());
        Route route = createRoute(5);
        createPrice(route.tripId(), 1, 3, 2, 12345);
        createPrice(route.tripId(), 1, 3, 1, 6789);

        String key = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(pricedBody(route.tripId(), passenger.id(), 2)))
                .andExpect(status().isCreated());

        // Changing the seat type under the same key is a different request.
        mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(pricedBody(route.tripId(), passenger.id(), 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("幂等键已用于不同的下单请求"));

        // The legacy entry is a different purchase mode.
        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderCreateRequest(
                                        route.tripId(), passenger.id()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("幂等键已用于不同的下单请求"));

        // The legacy entry still creates an order without any amount.
        String legacyKey = UUID.randomUUID().toString();
        OrderCreateResponse legacy = createLegacyOrder(
                session, route.tripId(), passenger.id(), legacyKey
        );

        assertNull(legacy.seatType());
        assertNull(legacy.amount());

        Map<String, Object> legacyRow = jdbcTemplate.queryForMap("""
                SELECT purchase_mode, departure_sequence, seat_type, amount_cents
                FROM ticket_orders
                WHERE id = ?
                """, legacy.id());

        assertEquals("LEGACY", legacyRow.get("purchase_mode"));
        assertNull(legacyRow.get("departure_sequence"));
        assertNull(legacyRow.get("seat_type"));
        assertNull(legacyRow.get("amount_cents"));

        // A legacy key must not be reused for the priced entry.
        mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", legacyKey)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(pricedBody(route.tripId(), passenger.id(), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("幂等键已用于不同的下单请求"));

        // The legacy replay still returns the original order.
        MvcResult replay = mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", legacyKey)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderCreateRequest(route.tripId(), passenger.id()))))
                .andExpect(status().isOk())
                .andReturn();

        OrderCreateResponse replayed = objectMapper.readValue(
                replay.getResponse().getContentAsByteArray(),
                OrderCreateResponse.class
        );

        assertEquals(legacy.id(), replayed.id());
        assertNull(replayed.amount());
    }

    @Test
    void shouldKeepSnapshotOnCancelAndTimeoutClose() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session, randomPassenger());
        Route route = createRoute(5);
        createPrice(route.tripId(), 1, 3, 2, 12345);

        OrderCreateResponse toCancel = createPricedOrder(
                session, route.tripId(), passenger.id(), 2
        );
        OrderCreateResponse toExpire = createPricedOrder(
                session, route.tripId(), passenger.id(), 2
        );

        Map<String, Object> before = snapshotOf(toCancel.id());

        mockMvc.perform(post("/api/orders/{id}/cancel", toCancel.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.amount").value(123.45));

        assertEquals(4, remainingTickets(route.tripId()));
        assertEquals(before, snapshotOf(toCancel.id()));

        // A repeated cancellation must not restore stock twice.
        mockMvc.perform(post("/api/orders/{id}/cancel", toCancel.id())
                        .session(session))
                .andExpect(status().isOk());

        assertEquals(4, remainingTickets(route.tripId()));

        // The timeout scan closes once and restores once.
        moveDeadlineToPast(toExpire.id());

        assertTrue(ticketOrderService.closeExpired(toExpire.id()));
        assertFalse(ticketOrderService.closeExpired(toExpire.id()));

        assertEquals(5, remainingTickets(route.tripId()));
        assertEquals(before, snapshotOf(toExpire.id()));

        mockMvc.perform(get("/api/orders/{id}", toExpire.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"))
                .andExpect(jsonPath("$.seatType").value(2))
                .andExpect(jsonPath("$.amount").value(123.45));
    }

    @Test
    void shouldRollBackPricedOrderWhenInsertFails() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session, randomPassenger());
        Route route = createRoute(5);
        createPrice(route.tripId(), 1, 3, 2, 12345);

        String key = UUID.randomUUID().toString();

        doThrow(new IllegalStateException("insert failed"))
                .when(ticketOrderMapper).insert(any());

        try {
            mockMvc.perform(post("/api/orders/priced")
                            .header("Idempotency-Key", key)
                            .session(session)
                            .contentType(APPLICATION_JSON)
                            .content(pricedBody(
                                    route.tripId(), passenger.id(), 2)))
                    .andExpect(status().isInternalServerError());
        } finally {
            reset(ticketOrderMapper);
        }

        // The whole unit of work must disappear together.
        assertEquals(5, remainingTickets(route.tripId()));
        assertEquals(0, orderCount(route.tripId()));
        assertEquals(0, idempotencyCount(key));
    }

    @Test
    void shouldKeepOneOrderAndStockUnderConcurrency() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(session, randomPassenger());

        // The same key may create only one order.
        Route sameKeyRoute = createRoute(5);
        createPrice(sameKeyRoute.tripId(), 1, 3, 2, 12345);

        String key = UUID.randomUUID().toString();

        List<Integer> sameKeyStatuses = runConcurrently(8, () ->
                mockMvc.perform(post("/api/orders/priced")
                                .header("Idempotency-Key", key)
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(pricedBody(
                                        sameKeyRoute.tripId(),
                                        passenger.id(), 2)))
                        .andReturn().getResponse().getStatus());

        assertEquals(1, Collections.frequency(sameKeyStatuses, 201));
        assertEquals(7, Collections.frequency(sameKeyStatuses, 200));
        assertEquals(1, orderCount(sameKeyRoute.tripId()));
        assertEquals(4, remainingTickets(sameKeyRoute.tripId()));

        // Three tickets can never become more than three orders.
        Route limitedRoute = createRoute(3);
        createPrice(limitedRoute.tripId(), 1, 3, 2, 12345);

        List<Integer> stockStatuses = runConcurrently(10, () ->
                mockMvc.perform(post("/api/orders/priced")
                                .header("Idempotency-Key",
                                        UUID.randomUUID().toString())
                                .session(session)
                                .contentType(APPLICATION_JSON)
                                .content(pricedBody(
                                        limitedRoute.tripId(),
                                        passenger.id(), 2)))
                        .andReturn().getResponse().getStatus());

        assertEquals(3, Collections.frequency(stockStatuses, 201));
        assertEquals(7, Collections.frequency(stockStatuses, 409));
        assertEquals(3, orderCount(limitedRoute.tripId()));
        assertEquals(0, remainingTickets(limitedRoute.tripId()));
    }

    private record Route(
            Long tripId,
            String firstStationCode,
            String lastStationCode,
            String firstName,
            String lastName,
            LocalDateTime departureAt,
            LocalDateTime arrivalAt
    ) {
    }

    private Route createRoute(int remainingTickets) {
        String suffix = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();

        String firstCode = "S" + suffix + "A";
        String middleCode = "S" + suffix + "M";
        String lastCode = "S" + suffix + "Z";

        String firstName = "测试起点" + suffix;
        String middleName = "测试中途" + suffix;
        String lastName = "测试终点" + suffix;

        insertStation(firstCode, firstName);
        insertStation(middleCode, middleName);
        insertStation(lastCode, lastName);

        LocalDateTime departureAt = LocalDateTime.of(2099, 5, 1, 8, 0);
        LocalDateTime middleArrival = LocalDateTime.of(2099, 5, 1, 10, 0);
        LocalDateTime middleDeparture = LocalDateTime.of(2099, 5, 1, 10, 10);
        LocalDateTime arrivalAt = LocalDateTime.of(2099, 5, 1, 12, 0);

        String trainNumber = "P" + suffix;

        jdbcTemplate.update("""
                INSERT INTO train_trips (
                    train_number, departure_station, arrival_station,
                    departure_at, arrival_at
                )
                VALUES (?, ?, ?, ?, ?)
                """, trainNumber, firstName, lastName, departureAt, arrivalAt);

        Long tripId = jdbcTemplate.queryForObject("""
                SELECT id FROM train_trips WHERE train_number = ?
                """, Long.class, trainNumber);

        insertStop(tripId, firstCode, 1, null, departureAt);
        insertStop(tripId, middleCode, 2, middleArrival, middleDeparture);
        insertStop(tripId, lastCode, 3, arrivalAt, null);

        jdbcTemplate.update("""
                INSERT INTO ticket_inventory (
                    train_trip_id, total_tickets, remaining_tickets
                )
                VALUES (?, ?, ?)
                """, tripId, remainingTickets, remainingTickets);

        return new Route(
                tripId, firstCode, lastCode, firstName, lastName,
                departureAt, arrivalAt
        );
    }

    private void insertStation(String code, String name) {
        jdbcTemplate.update("""
                INSERT INTO stations (code, name, spell, region_name)
                VALUES (?, ?, ?, ?)
                """, code, name, code.toLowerCase(), "测试区域");
    }

    private void insertStop(
            Long tripId,
            String stationCode,
            int sequence,
            LocalDateTime arrivalAt,
            LocalDateTime departureAt
    ) {
        Long stationId = jdbcTemplate.queryForObject("""
                SELECT id FROM stations WHERE code = ?
                """, Long.class, stationCode);

        jdbcTemplate.update("""
                INSERT INTO train_trip_stops (
                    train_trip_id, station_id, stop_sequence,
                    arrival_at, departure_at
                )
                VALUES (?, ?, ?, ?, ?)
                """, tripId, stationId, sequence, arrivalAt, departureAt);
    }

    private void createPrice(
            Long tripId, int fromSequence, int toSequence,
            int seatType, int priceCents
    ) {
        jdbcTemplate.update("""
                INSERT INTO train_trip_interval_prices (
                    train_trip_id, departure_sequence, arrival_sequence,
                    seat_type, price_cents
                )
                VALUES (?, ?, ?, ?, ?)
                """, tripId, fromSequence, toSequence, seatType, priceCents);
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

    private int amountCentsOf(Long orderId) {
        Integer cents = jdbcTemplate.queryForObject("""
                SELECT amount_cents FROM ticket_orders WHERE id = ?
                """, Integer.class, orderId);
        assertNotNull(cents);
        return cents;
    }

    private OrderCreateResponse createPricedOrder(
            MockHttpSession session,
            Long tripId,
            Long passengerId,
            int seatType
    ) throws Exception {
        String body = """
                {"trainTripId": %d, "passengerId": %d, "seatType": %d}
                """.formatted(tripId, passengerId, seatType);

        MvcResult result = mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                OrderCreateResponse.class
        );
    }

    private PassengerSaveRequest randomPassenger() {
        String digits = String.format("%04d",
                ThreadLocalRandom.current().nextInt(10000));

        return new PassengerSaveRequest(
                "Priced Rider " + digits,
                "0000000000000" + digits + "X",
                "1390000" + digits
        );
    }

    private MockHttpSession registerAndLogin() throws Exception {
        String username = "priced_" + UUID.randomUUID()
                .toString().replace("-", "");
        String credentials = objectMapper.writeValueAsString(
                new Credentials(username, "Train123")
        );

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

    private PassengerResponse createPassenger(
            MockHttpSession session,
            PassengerSaveRequest request
    ) throws Exception {
        mockMvc.perform(post("/api/passengers")
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
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

    private String pricedBody(Long tripId, Long passengerId, int seatType) {
        return """
                {"trainTripId": %d, "passengerId": %d, "seatType": %d}
                """.formatted(tripId, passengerId, seatType);
    }

    private Long createBareTrip(String departureStation, String arrivalStation) {
        String suffix = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        String trainNumber = "B" + suffix;

        jdbcTemplate.update("""
                INSERT INTO train_trips (
                    train_number, departure_station, arrival_station,
                    departure_at, arrival_at
                )
                VALUES (?, ?, ?, ?, ?)
                """, trainNumber, departureStation, arrivalStation,
                LocalDateTime.of(2099, 6, 1, 8, 0),
                LocalDateTime.of(2099, 6, 1, 12, 0));

        Long tripId = jdbcTemplate.queryForObject("""
                SELECT id FROM train_trips WHERE train_number = ?
                """, Long.class, trainNumber);

        jdbcTemplate.update("""
                INSERT INTO ticket_inventory (
                    train_trip_id, total_tickets, remaining_tickets
                )
                VALUES (?, 5, 5)
                """, tripId);

        return tripId;
    }

    private void assertStopsRejected(
            MockHttpSession session,
            Long passengerId,
            Long tripId,
            int expectedRemaining
    ) throws Exception {
        String key = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/orders/priced")
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(pricedBody(tripId, passengerId, 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("该车次全程经停资料不完整，无法下单"));

        assertEquals(expectedRemaining, remainingTickets(tripId));
        assertEquals(0, orderCount(tripId));
        assertEquals(0, idempotencyCount(key));
    }

    private long orderCount(Long tripId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM ticket_orders WHERE train_trip_id = ?
                """, Long.class, tripId);
        assertNotNull(count);
        return count;
    }

    private long idempotencyCount(String key) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM order_idempotency
                WHERE idempotency_key = ?
                """, Long.class, key);
        assertNotNull(count);
        return count;
    }

    private OrderCreateResponse createLegacyOrder(
            MockHttpSession session,
            Long tripId,
            Long passengerId,
            String key
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", key)
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderCreateRequest(tripId, passengerId))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                OrderCreateResponse.class
        );
    }

    private Map<String, Object> snapshotOf(Long orderId) {
        return jdbcTemplate.queryForMap("""
                SELECT departure_sequence, arrival_sequence,
                       departure_station_code, departure_station_name,
                       departure_at, arrival_station_code,
                       arrival_station_name, arrival_at,
                       seat_type, amount_cents
                FROM ticket_orders
                WHERE id = ?
                """, orderId);
    }

    private void moveDeadlineToPast(Long orderId) {
        jdbcTemplate.update("""
                UPDATE ticket_orders
                SET pay_deadline = DATE_SUB(NOW(), INTERVAL 1 MINUTE)
                WHERE id = ?
                """, orderId);
    }

    private List<Integer> runConcurrently(
            int threads, Callable<Integer> action
    ) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for(int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return action.call();
                }));
            }

            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for(Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    private record Credentials(String username, String password) {

    }
}
