package com.mia.mini12306.order;

import com.mia.mini12306.inventory.InsufficientTicketsException;
import com.mia.mini12306.inventory.TicketInventory;
import com.mia.mini12306.inventory.mapper.TicketInventoryMapper;
import com.mia.mini12306.order.dto.OrderCreateRequest;
import com.mia.mini12306.order.dto.OrderCreateResponse;
import com.mia.mini12306.order.dto.OrderPageResponse;
import com.mia.mini12306.order.mapper.TicketOrderMapper;
import com.mia.mini12306.passenger.dto.PassengerResponse;
import com.mia.mini12306.passenger.dto.PassengerSaveRequest;
import com.mia.mini12306.user.CurrentUserSession;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TicketOrderIntegrationTests {

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

    @MockitoSpyBean
    private TicketInventoryMapper ticketInventoryMapper;

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    @Test
    void shouldCreateOrderAndDecreaseInventory() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID
        );
        assertNotNull(userId);

        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Order Rider", "00000000000000001x", "13800000000"
                )
        );

        // Avoid hiding swapped arguments behind equal IDs.
        if (passenger.id().equals(userId)) {
            passenger = createPassenger(
                    session,
                    new PassengerSaveRequest(
                            "Backup Rider", "00000000000000002x", "13800000001"
                    )
            );
        }
        assertNotEquals(userId, passenger.id());

        Long tripId = createTrip();
        createInventory(tripId, 2, 2);

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderCreateRequest(tripId, passenger.id())
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.trainTripId").value(tripId))
                .andExpect(jsonPath("$.passengerId").value(passenger.id()))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.passengerName").doesNotExist())
                .andExpect(jsonPath("$.passengerIdCard").doesNotExist())
                .andReturn();

        OrderCreateResponse response = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                OrderCreateResponse.class
        );
        assertTrue(response.id() > 0);
        assertNotNull(response.createdAt());

        // Verify committed data independently of the application's Mapper.
        var saved = jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                response.id()
        );

        assertEquals(userId.longValue(),
                ((Number) saved.get("user_id")).longValue());
        assertEquals(tripId.longValue(),
                ((Number) saved.get("train_trip_id")).longValue());
        assertEquals(passenger.id().longValue(),
                ((Number) saved.get("passenger_id")).longValue());
        assertEquals(passenger.realName(), saved.get("passenger_name"));
        assertEquals(passenger.idCard(), saved.get("passenger_id_card"));
        assertEquals("PENDING_PAYMENT", saved.get("status"));
        assertNotNull(saved.get("created_at"));
        assertNotNull(saved.get("updated_at"));

        Integer remainingTickets = jdbcTemplate.queryForObject("""
                SELECT remaining_tickets
                FROM ticket_inventory
                WHERE train_trip_id = ?
                """, Integer.class, tripId);

        assertNotNull(remainingTickets);
        assertEquals(1, remainingTickets.intValue());

        Long orderCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ticket_orders
                WHERE train_trip_id = ?
                """, Long.class, tripId);

        assertNotNull(orderCount);
        assertEquals(1L, orderCount.longValue());

        // The public query must reflect the committed decrease.
        mockMvc.perform(get("/api/trains/{id}/inventory", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingTickets").value(1));
    }

    @Test
    void shouldRejectOrderWhenSoldOut() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Stock Rider", "00000000000000003X", "13800000003"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 1, 1);

        String body = objectMapper.writeValueAsString(
                new OrderCreateRequest(tripId, passenger.id())
        );

        // The first order consumes the last ticket.
        mockMvc.perform(post("/api/orders")
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        assertInventoryAndOrderCount(tripId, 0, 1);

        // A rejected order must not create another row or negative stock.
        mockMvc.perform(post("/api/orders")
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("余票不足"));

        assertInventoryAndOrderCount(tripId, 0, 1);
    }

    @Test
    void shouldRejectPassengerOwnedByAnotherAccount() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        MockHttpSession otherSession = registerAndLogin();

        Long otherUserId = (Long) otherSession.getAttribute(
                CurrentUserSession.LOGIN_USER_ID
        );
        assertNotNull(otherUserId);

        PassengerResponse target = createPassenger(
                ownerSession,
                new PassengerSaveRequest(
                        "Owner Rider", "00000000000000004X", "13800000004"
                )
        );

        // Keep the passenger ID different from the requesting account ID.
        if (target.id().equals(otherUserId)) {
            target = createPassenger(
                    ownerSession,
                    new PassengerSaveRequest(
                            "Backup Rider", "00000000000000005X", "13800000005"
                    )
            );
        }
        assertNotEquals(otherUserId, target.id());

        // The other account may have its own record for the same document.
        PassengerResponse otherPassenger = createPassenger(
                otherSession,
                new PassengerSaveRequest(
                        "Other Rider", target.idCard(), "13800000006"
                )
        );
        assertNotEquals(target.id(), otherPassenger.id());

        Long tripId = createTrip();
        createInventory(tripId, 2, 2);

        mockMvc.perform(post("/api/orders")
                        .session(otherSession)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderCreateRequest(tripId, target.id())
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("乘车人不存在"));

        assertInventoryAndOrderCount(tripId, 2, 0);
    }

    @Test
    void shouldRejectOrderWithoutLogin() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Login Rider", "00000000000000006X", "13800000006"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 2, 2);

        // Omit the session while keeping the request data valid.
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderCreateRequest(tripId, passenger.id())
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"))
                .andReturn();

        assertNull(result.getRequest().getSession(false));
        assertInventoryAndOrderCount(tripId, 2, 0);
    }

    @Test
    void shouldRejectInvalidOrderIds() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Input Rider", "00000000000000007X", "13800000007"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 2, 2);

        OrderCreateRequest[] invalidRequests = {
                new OrderCreateRequest(null, passenger.id()),
                new OrderCreateRequest(0L, passenger.id()),
                new OrderCreateRequest(-1L, passenger.id()),
                new OrderCreateRequest(tripId, null),
                new OrderCreateRequest(tripId, 0L),
                new OrderCreateRequest(tripId, -1L)
        };

        // Isolate each validation rule with an authenticated request.
        for (OrderCreateRequest request : invalidRequests) {
            mockMvc.perform(post("/api/orders")
                            .session(session)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            assertInventoryAndOrderCount(tripId, 2, 0);
        }
    }

    @Test
    void shouldRejectMissingPassengerOrInventory() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Missing Rider", "00000000000000008X", "13800000008"
                )
        );

        Long configuredTripId = createTrip();
        createInventory(configuredTripId, 2, 2);

        // A missing passenger must not consume stock.
        mockMvc.perform(post("/api/orders")
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderCreateRequest(
                                        configuredTripId, Long.MAX_VALUE
                                )
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("乘车人不存在"));

        assertInventoryAndOrderCount(configuredTripId, 2, 0);

        Long tripWithoutInventory = createTrip();

        // Cover both an unconfigured trip and a nonexistent trip.
        for (Long tripId : new Long[]{
                tripWithoutInventory, Long.MAX_VALUE
        }) {
            mockMvc.perform(post("/api/orders")
                            .session(session)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new OrderCreateRequest(tripId, passenger.id())
                            )))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value("车次不存在或未配置库存"));

            Long inventoryCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ticket_inventory
                WHERE train_trip_id = ?
                """, Long.class, tripId);

            assertNotNull(inventoryCount);
            assertEquals(0L, inventoryCount.longValue());

            Long orderCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ticket_orders
                WHERE train_trip_id = ?
                """, Long.class, tripId);

            assertNotNull(orderCount);
            assertEquals(0L, orderCount.longValue());
        }
    }

    @Test
    void shouldRollBackInventoryWhenOrderInsertFails() throws Exception {
        MockHttpSession session = registerAndLogin();
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID
        );
        assertNotNull(userId);

        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Rollback Rider", "00000000000000009X", "13800000009"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 2, 2);

        DataIntegrityViolationException simulatedFailure =
                new DataIntegrityViolationException(
                        "Simulated order insert failure"
                );

        doAnswer(invocation -> {
            // Confirm that the real service transaction is active.
            assertTrue(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );

            // Observe the real decrease before triggering the failure.
            Integer remaining = jdbcTemplate.queryForObject("""
                SELECT remaining_tickets
                FROM ticket_inventory
                WHERE train_trip_id = ?
                """, Integer.class, tripId);

            assertNotNull(remaining);
            assertEquals(1, remaining.intValue());

            throw simulatedFailure;
        }).when(ticketOrderMapper).insert(any(TicketOrder.class));

        // No test transaction may hide a missing service transaction.
        assertFalse(
                TransactionSynchronizationManager.isActualTransactionActive()
        );

        DataIntegrityViolationException actual = assertThrows(
                DataIntegrityViolationException.class,
                () -> ticketOrderService.create(
                        userId,
                        new OrderCreateRequest(tripId, passenger.id())
                )
        );

        assertSame(simulatedFailure, actual);
        verify(ticketOrderMapper).insert(any(TicketOrder.class));

        // Check the database after the service transaction has rolled back.
        assertFalse(
                TransactionSynchronizationManager.isActualTransactionActive()
        );
        assertInventoryAndOrderCount(tripId, 2, 0);
    }

    @Test
    void shouldNotOversellUnderConcurrentOrders() throws Exception {
        // Fixtures must be committed before worker threads access them.
        assertFalse(
                TransactionSynchronizationManager.isActualTransactionActive()
        );

        MockHttpSession session = registerAndLogin();
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID
        );
        assertNotNull(userId);

        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Concurrent Rider", "00000000000000010X", "13800000010"
                )
        );

        Long tripId = createTrip();
        int initialStock = 3;
        int requestCount = 8;
        createInventory(tripId, initialStock, initialStock);

        OrderCreateRequest request =
                new OrderCreateRequest(tripId, passenger.id());

        var executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<TicketOrder>> futures = new ArrayList<>();

        int succeeded = 0;
        int rejected = 0;

        try {
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Start signal timed out");
                    }

                    // Each thread enters its own service transaction.
                    return ticketOrderService.create(userId, request);
                }));
            }

            assertTrue(
                    ready.await(15, TimeUnit.SECONDS),
                    "Workers did not become ready in time"
            );

            long deadline =
                    System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            start.countDown();

            for (Future<TicketOrder> future : futures) {
                long remainingNanos = deadline - System.nanoTime();
                assertTrue(remainingNanos > 0, "Concurrent orders timed out");

                try {
                    TicketOrder order = future.get(
                            remainingNanos, TimeUnit.NANOSECONDS
                    );

                    assertNotNull(order.getId());
                    assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
                    succeeded++;
                } catch (ExecutionException exception) {
                    // Unexpected database or application errors must fail the test.
                    assertInstanceOf(
                            InsufficientTicketsException.class,
                            exception.getCause()
                    );
                    rejected++;
                }
            }
        } finally {
            // Release waiting workers even if setup or assertions fail.
            start.countDown();
            executor.shutdownNow();
            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Worker threads did not terminate"
            );
        }

        assertEquals(initialStock, succeeded);
        assertEquals(requestCount - initialStock, rejected);
        assertInventoryAndOrderCount(tripId, 0, initialStock);

        // All committed orders must retain the correct ownership and state.
        Long matchingOrders = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM ticket_orders
            WHERE train_trip_id = ?
              AND user_id = ?
              AND passenger_id = ?
              AND status = 'PENDING_PAYMENT'
            """, Long.class, tripId, userId, passenger.id());

        assertNotNull(matchingOrders);
        assertEquals((long) initialStock, matchingOrders.longValue());
    }

    @Test
    void shouldQueryOwnOrdersNewestFirst() throws Exception {
        MockHttpSession session = registerAndLogin();

        // A new account has an empty first page.
        mockMvc.perform(get("/api/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Query Rider",
                        "00000000000000011X",
                        "13800000011"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 3, 3);

        OrderCreateResponse first =
                createOrder(session, tripId, passenger.id());
        OrderCreateResponse second =
                createOrder(session, tripId, passenger.id());

        assertTrue(second.id() > first.id());
        assertInventoryAndOrderCount(tripId, 1, 2);

        // Later passenger edits must not change order snapshots.
        mockMvc.perform(put("/api/passengers/{id}", passenger.id())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PassengerSaveRequest(
                                        "Updated Rider",
                                        passenger.idCard(),
                                        "13900000011"
                                )
                        )))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(second.id()))
                .andExpect(jsonPath("$.items[1].id").value(first.id()))
                .andExpect(jsonPath("$.items[0].trainTripId").value(tripId))
                .andExpect(jsonPath("$.items[0].passengerId")
                        .value(passenger.id()))
                .andExpect(jsonPath("$.items[0].passengerName")
                        .value("Query Rider"))
                .andExpect(jsonPath("$.items[0].status")
                        .value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.items[0].createdAt").exists())
                .andExpect(jsonPath("$.items[0].updatedAt").exists())
                .andExpect(jsonPath("$.items[*].userId").isEmpty())
                .andExpect(jsonPath("$.items[*].passengerIdCard").isEmpty())
                .andExpect(jsonPath("$.items[*].idCard").isEmpty());

        // Query both IDs so equal user/order IDs cannot hide swapped arguments.
        for (OrderCreateResponse order :
                new OrderCreateResponse[]{first, second}) {
            mockMvc.perform(get("/api/orders/{id}", order.id())
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(order.id()))
                    .andExpect(jsonPath("$.trainTripId").value(tripId))
                    .andExpect(jsonPath("$.passengerId")
                            .value(passenger.id()))
                    .andExpect(jsonPath("$.passengerName")
                            .value("Query Rider"))
                    .andExpect(jsonPath("$.status")
                            .value("PENDING_PAYMENT"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.userId").doesNotExist())
                    .andExpect(jsonPath("$.passengerIdCard").doesNotExist())
                    .andExpect(jsonPath("$.idCard").doesNotExist());
        }

        // Reading orders must not change stock or create extra orders.
        assertInventoryAndOrderCount(tripId, 1, 2);
    }

    @Test
    void shouldRejectOrderQueriesWithoutLogin() throws Exception {
        for (String url : new String[]{
                "/api/orders",
                "/api/orders/" + Long.MAX_VALUE
        }) {
            // Authentication must happen before reading order data.
            MvcResult result = mockMvc.perform(get(url))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("请先登录"))
                    .andReturn();

            assertNull(result.getRequest().getSession(false));
        }
    }

    @Test
    void shouldIsolateOrderQueriesBetweenAccounts() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        MockHttpSession otherSession = registerAndLogin();

        Long ownerUserId = (Long) ownerSession.getAttribute(
                CurrentUserSession.LOGIN_USER_ID
        );
        assertNotNull(ownerUserId);

        // The same identity document does not grant access across accounts.
        PassengerResponse ownerPassenger = createPassenger(
                ownerSession,
                new PassengerSaveRequest(
                        "Owner Rider",
                        "00000000000000012X",
                        "13800000012"
                )
        );
        PassengerResponse otherPassenger = createPassenger(
                otherSession,
                new PassengerSaveRequest(
                        "Other Rider",
                        "00000000000000012X",
                        "13800000013"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 4, 4);

        OrderCreateResponse first =
                createOrder(ownerSession, tripId, ownerPassenger.id());
        OrderCreateResponse second =
                createOrder(ownerSession, tripId, ownerPassenger.id());
        OrderCreateResponse other =
                createOrder(otherSession, tripId, otherPassenger.id());

        var before = jdbcTemplate.queryForList("""
            SELECT *
            FROM ticket_orders
            WHERE train_trip_id = ?
            ORDER BY id
            """, tripId);

        mockMvc.perform(get("/api/orders")
                        .session(ownerSession)
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(second.id()))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/orders")
                        .session(ownerSession)
                        .param("page", "2")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(first.id()))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        // A supplied userId must not override the session identity.
        mockMvc.perform(get("/api/orders")
                        .session(otherSession)
                        .param("page", "1")
                        .param("size", "1")
                        .param("userId", ownerUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(other.id()))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        for (OrderCreateResponse order :
                new OrderCreateResponse[]{first, second}) {
            mockMvc.perform(get("/api/orders/{id}", order.id())
                            .session(otherSession))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("订单不存在"));
        }

        // Check isolation in the opposite direction as well.
        mockMvc.perform(get("/api/orders/{id}", other.id())
                        .session(ownerSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("订单不存在"));

        mockMvc.perform(get("/api/orders/{id}", other.id())
                        .session(otherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(other.id()));

        assertEquals(before, jdbcTemplate.queryForList("""
            SELECT *
            FROM ticket_orders
            WHERE train_trip_id = ?
            ORDER BY id
            """, tripId));
        assertInventoryAndOrderCount(tripId, 1, 3);
    }

    @Test
    void shouldRejectMissingOrInvalidOrderQueryIds() throws Exception {
        MockHttpSession session = registerAndLogin();

        // A valid ID with no matching order is a business 404.
        mockMvc.perform(get("/api/orders/{id}", Long.MAX_VALUE)
                        .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("订单不存在"));

        // Invalid IDs are rejected by request validation or conversion.
        for (String id : new String[]{"0", "-1", "abc"}) {
            mockMvc.perform(get("/api/orders/{id}", id)
                            .session(session))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void shouldPayOrderIdempotentlyAndRejectCancellation() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Payment Rider",
                        "00000000000000014X",
                        "13800000014"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 2, 2);
        OrderCreateResponse order =
                createOrder(session, tripId, passenger.id());

        assertInventoryAndOrderCount(tripId, 1, 1);

        mockMvc.perform(post("/api/orders/{id}/pay", order.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.id()))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.passengerIdCard").doesNotExist());

        var paid = jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                order.id()
        );
        assertEquals("PAID", paid.get("status"));
        assertInventoryAndOrderCount(tripId, 1, 1);

        // Repeated payment must preserve the committed result.
        mockMvc.perform(post("/api/orders/{id}/pay", order.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        assertInventoryAndOrderCount(tripId, 1, 1);

        // Cancelling a paid order would require a separate refund flow.
        mockMvc.perform(post("/api/orders/{id}/cancel", order.id())
                        .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("当前订单状态不允许取消"));

        assertEquals(paid, jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                order.id()
        ));
        assertInventoryAndOrderCount(tripId, 1, 1);

        mockMvc.perform(get("/api/orders/{id}", order.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(get("/api/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(order.id()))
                .andExpect(jsonPath("$.items[0].status").value("PAID"));
    }

    @Test
    void shouldCancelOrderOnlyOnceAndRejectPayment() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Cancel Rider",
                        "00000000000000015X",
                        "13800000015"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 3, 3);

        OrderCreateResponse target =
                createOrder(session, tripId, passenger.id());
        OrderCreateResponse retained =
                createOrder(session, tripId, passenger.id());

        assertInventoryAndOrderCount(tripId, 1, 2);

        mockMvc.perform(post("/api/orders/{id}/cancel", target.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target.id()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.passengerIdCard").doesNotExist());

        var cancelled = jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                target.id()
        );
        assertEquals("CANCELLED", cancelled.get("status"));
        assertInventoryAndOrderCount(tripId, 2, 2);

        // Spare capacity must not allow a duplicate stock restoration.
        mockMvc.perform(post("/api/orders/{id}/cancel", target.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertInventoryAndOrderCount(tripId, 2, 2);

        mockMvc.perform(post("/api/orders/{id}/pay", target.id())
                        .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("当前订单状态不允许支付"));

        assertEquals(cancelled, jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                target.id()
        ));
        assertInventoryAndOrderCount(tripId, 2, 2);

        // Cancelling one order must not change another reservation.
        String retainedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM ticket_orders WHERE id = ?",
                String.class,
                retained.id()
        );
        assertEquals("PENDING_PAYMENT", retainedStatus);

        mockMvc.perform(get("/api/orders/{id}", target.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(retained.id()))
                .andExpect(jsonPath("$.items[0].status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.items[1].id").value(target.id()))
                .andExpect(jsonPath("$.items[1].status").value("CANCELLED"));

        mockMvc.perform(get("/api/trains/{id}/inventory", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingTickets").value(2));
    }

    @Test
    void shouldRejectOrderActionsWithoutLogin() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Anonymous Rider",
                        "00000000000000016X",
                        "13800000016"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 2, 2);
        OrderCreateResponse order =
                createOrder(session, tripId, passenger.id());

        var before = jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                order.id()
        );

        for (String action : new String[]{"pay", "cancel"}) {
            // Omit the session while targeting a real pending order.
            MvcResult result = mockMvc.perform(
                            post("/api/orders/{id}/{action}",
                                    order.id(), action))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("请先登录"))
                    .andReturn();

            assertNull(result.getRequest().getSession(false));
            assertEquals(before, jdbcTemplate.queryForMap(
                    "SELECT * FROM ticket_orders WHERE id = ?",
                    order.id()
            ));
            assertInventoryAndOrderCount(tripId, 1, 1);
        }
    }

    @Test
    void shouldRejectOrderActionsFromAnotherAccount() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        MockHttpSession otherSession = registerAndLogin();

        Long ownerUserId = (Long) ownerSession.getAttribute(
                CurrentUserSession.LOGIN_USER_ID
        );
        assertNotNull(ownerUserId);

        PassengerResponse passenger = createPassenger(
                ownerSession,
                new PassengerSaveRequest(
                        "Protected Rider",
                        "00000000000000017X",
                        "13800000017"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 5, 5);

        OrderCreateResponse pending =
                createOrder(ownerSession, tripId, passenger.id());
        OrderCreateResponse paid =
                createOrder(ownerSession, tripId, passenger.id());
        OrderCreateResponse cancelled =
                createOrder(ownerSession, tripId, passenger.id());

        // Prepare terminal states through the real owner endpoints.
        mockMvc.perform(post("/api/orders/{id}/pay", paid.id())
                        .session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(post("/api/orders/{id}/cancel", cancelled.id())
                        .session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        var before = jdbcTemplate.queryForList("""
            SELECT *
            FROM ticket_orders
            WHERE train_trip_id = ?
            ORDER BY id
            """, tripId);

        assertInventoryAndOrderCount(tripId, 3, 3);

        for (OrderCreateResponse order :
                new OrderCreateResponse[]{pending, paid, cancelled}) {
            for (String action : new String[]{"pay", "cancel"}) {
                // Even idempotent retries must enforce ownership first.
                mockMvc.perform(post("/api/orders/{id}/{action}",
                                order.id(), action)
                                .session(otherSession)
                                .param("userId", ownerUserId.toString()))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.message").value("订单不存在"));

                assertEquals(before, jdbcTemplate.queryForList("""
                    SELECT *
                    FROM ticket_orders
                    WHERE train_trip_id = ?
                    ORDER BY id
                    """, tripId));
                assertInventoryAndOrderCount(tripId, 3, 3);
            }
        }

        // Rejected access must leave the owner's pending order unchanged.
        mockMvc.perform(get("/api/orders/{id}", pending.id())
                        .session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void shouldRejectMissingOrInvalidOrderActionIds() throws Exception {
        MockHttpSession session = registerAndLogin();

        for (String action : new String[]{"pay", "cancel"}) {
            // Missing orders use the same response as inaccessible orders.
            mockMvc.perform(post("/api/orders/{id}/{action}",
                            Long.MAX_VALUE, action)
                            .session(session))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("订单不存在"));

            for (String id : new String[]{"0", "-1", "abc"}) {
                mockMvc.perform(post("/api/orders/{id}/{action}",
                                id, action)
                                .session(session))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Test
    void shouldRollBackCancellationAndRestoredInventoryOnFailure()
            throws Exception {
        MockHttpSession session = registerAndLogin();
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID
        );
        assertNotNull(userId);

        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Rollback Cancel",
                        "00000000000000018X",
                        "13800000018"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 2, 2);
        OrderCreateResponse order =
                createOrder(session, tripId, passenger.id());

        var before = jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                order.id()
        );
        assertEquals("PENDING_PAYMENT", before.get("status"));
        assertInventoryAndOrderCount(tripId, 1, 1);

        DataIntegrityViolationException simulatedFailure =
                new DataIntegrityViolationException(
                        "Simulated failure after stock restoration"
                );

        doAnswer(invocation -> {
            // Confirm that the real service transaction is active.
            assertTrue(
                    TransactionSynchronizationManager
                            .isActualTransactionActive()
            );

            String currentStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM ticket_orders WHERE id = ?",
                    String.class,
                    order.id()
            );
            assertEquals("CANCELLED", currentStatus);

            // Execute the registered Mapper SQL within the current transaction.
            int restoredRows = sqlSessionTemplate.update(
                    TicketInventoryMapper.class.getName()
                            + ".restoreOneIfNotFull",
                    Map.of("trainTripId", tripId)
            );
            assertEquals(1, restoredRows);
            assertInventoryAndOrderCount(tripId, 2, 1);

            throw simulatedFailure;
        }).when(ticketInventoryMapper).restoreOneIfNotFull(tripId);

        // No test transaction may hide a missing service transaction.
        assertFalse(
                TransactionSynchronizationManager
                        .isActualTransactionActive()
        );

        DataIntegrityViolationException actual = assertThrows(
                DataIntegrityViolationException.class,
                () -> ticketOrderService.cancel(userId, order.id())
        );

        assertSame(simulatedFailure, actual);
        verify(ticketInventoryMapper).restoreOneIfNotFull(tripId);

        // Inspect committed data after the service transaction has rolled back.
        assertFalse(
                TransactionSynchronizationManager
                        .isActualTransactionActive()
        );
        assertEquals(before, jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                order.id()
        ));
        assertInventoryAndOrderCount(tripId, 1, 1);

        mockMvc.perform(get("/api/orders/{id}", order.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        mockMvc.perform(get("/api/trains/{id}/inventory", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingTickets").value(1));
    }

    @Test
    void shouldRollBackCancellationWhenStockRestorationReturnsZero()
            throws Exception {
        MockHttpSession session = registerAndLogin();
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID
        );
        assertNotNull(userId);

        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Restore Failure",
                        "00000000000000019X",
                        "13800000019"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 2, 2);
        OrderCreateResponse order =
                createOrder(session, tripId, passenger.id());

        var before = jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                order.id()
        );
        assertEquals("PENDING_PAYMENT", before.get("status"));
        assertInventoryAndOrderCount(tripId, 1, 1);

        doAnswer(invocation -> {
            assertTrue(
                    TransactionSynchronizationManager
                            .isActualTransactionActive()
            );

            // The order transition has happened before stock restoration.
            String currentStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM ticket_orders WHERE id = ?",
                    String.class,
                    order.id()
            );
            assertEquals("CANCELLED", currentStatus);
            assertInventoryAndOrderCount(tripId, 1, 1);

            // Simulate an update that restores no inventory row.
            return 0;
        }).when(ticketInventoryMapper).restoreOneIfNotFull(tripId);

        assertFalse(
                TransactionSynchronizationManager
                        .isActualTransactionActive()
        );

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> ticketOrderService.cancel(userId, order.id())
        );

        assertEquals(
                "Cancelled order inventory could not be restored",
                failure.getMessage()
        );
        verify(ticketInventoryMapper).restoreOneIfNotFull(tripId);

        // A zero-row restoration must roll back the order transition.
        assertFalse(
                TransactionSynchronizationManager
                        .isActualTransactionActive()
        );
        assertEquals(before, jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                order.id()
        ));
        assertInventoryAndOrderCount(tripId, 1, 1);

        mockMvc.perform(get("/api/orders/{id}", order.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void shouldRestoreStockOnceUnderConcurrentCancellations()
            throws Exception {
        // Fixtures must be committed before worker threads access them.
        assertFalse(
                TransactionSynchronizationManager
                        .isActualTransactionActive()
        );

        MockHttpSession session = registerAndLogin();
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID
        );
        assertNotNull(userId);

        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Concurrent Cancel",
                        "00000000000000020X",
                        "13800000020"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 10, 10);

        OrderCreateResponse target =
                createOrder(session, tripId, passenger.id());
        OrderCreateResponse retained =
                createOrder(session, tripId, passenger.id());

        var retainedBefore = jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                retained.id()
        );
        assertInventoryAndOrderCount(tripId, 8, 2);

        int requestCount = 8;
        var executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<TicketOrder>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Start signal timed out"
                        );
                    }

                    // Each worker enters a separate service transaction.
                    return ticketOrderService.cancel(userId, target.id());
                }));
            }

            assertTrue(
                    ready.await(15, TimeUnit.SECONDS),
                    "Workers did not become ready in time"
            );

            long deadline =
                    System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            start.countDown();

            for (Future<TicketOrder> future : futures) {
                long remainingNanos = deadline - System.nanoTime();
                assertTrue(
                        remainingNanos > 0,
                        "Concurrent cancellations timed out"
                );

                // Every retry must succeed; any worker exception fails the test.
                TicketOrder result = future.get(
                        remainingNanos, TimeUnit.NANOSECONDS
                );

                assertEquals(target.id(), result.getId());
                assertEquals(userId, result.getUserId());
                assertEquals(tripId, result.getTrainTripId());
                assertEquals(OrderStatus.CANCELLED, result.getStatus());
            }
        } finally {
            // Release waiting workers even when an assertion fails.
            start.countDown();
            executor.shutdownNow();
            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Worker threads did not terminate"
            );
        }

        // Eight successful requests must restore only one ticket.
        assertInventoryAndOrderCount(tripId, 9, 2);
        verify(ticketInventoryMapper).restoreOneIfNotFull(tripId);

        String targetStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM ticket_orders WHERE id = ?",
                String.class,
                target.id()
        );
        assertEquals("CANCELLED", targetStatus);

        assertEquals(retainedBefore, jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                retained.id()
        ));

        mockMvc.perform(get("/api/orders/{id}", target.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/trains/{id}/inventory", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingTickets").value(9));
    }

    @Test
    void shouldKeepPaymentAndCancellationMutuallyExclusive()
            throws Exception {
        assertFalse(
                TransactionSynchronizationManager
                        .isActualTransactionActive()
        );

        MockHttpSession session = registerAndLogin();
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID
        );
        assertNotNull(userId);

        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Payment Race",
                        "00000000000000021X",
                        "13800000021"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 10, 10);
        OrderCreateResponse target =
                createOrder(session, tripId, passenger.id());
        OrderCreateResponse retained =
                createOrder(session, tripId, passenger.id());

        var retainedBefore = jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                retained.id()
        );
        assertInventoryAndOrderCount(tripId, 8, 2);

        int requestCount = 8;
        var executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<TicketOrder>> futures = new ArrayList<>();

        int paidSucceeded = 0;
        int cancelledSucceeded = 0;
        int conflicts = 0;

        try {
            for (int i = 0; i < requestCount; i++) {
                boolean payment = i % 2 == 0;

                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Start signal timed out"
                        );
                    }

                    // Competing actions enter independent service transactions.
                    return payment
                            ? ticketOrderService.pay(userId, target.id())
                            : ticketOrderService.cancel(userId, target.id());
                }));
            }

            assertTrue(
                    ready.await(15, TimeUnit.SECONDS),
                    "Workers did not become ready in time"
            );

            long deadline =
                    System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            start.countDown();

            for (int i = 0; i < futures.size(); i++) {
                boolean payment = i % 2 == 0;
                long remainingNanos = deadline - System.nanoTime();
                assertTrue(
                        remainingNanos > 0,
                        "Concurrent actions timed out"
                );

                try {
                    TicketOrder result = futures.get(i).get(
                            remainingNanos, TimeUnit.NANOSECONDS
                    );

                    assertEquals(target.id(), result.getId());
                    assertEquals(userId, result.getUserId());
                    assertEquals(tripId, result.getTrainTripId());
                    assertEquals(
                            payment ? OrderStatus.PAID : OrderStatus.CANCELLED,
                            result.getStatus()
                    );

                    if (payment) {
                        paidSucceeded++;
                    } else {
                        cancelledSucceeded++;
                    }
                } catch (ExecutionException exception) {
                    // Database errors or unrelated exceptions must fail the test.
                    OrderStateConflictException conflict = assertInstanceOf(
                            OrderStateConflictException.class,
                            exception.getCause()
                    );
                    assertEquals(
                            payment
                                    ? "当前订单状态不允许支付"
                                    : "当前订单状态不允许取消",
                            conflict.getMessage()
                    );
                    conflicts++;
                }
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Worker threads did not terminate"
            );
        }

        String finalStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM ticket_orders WHERE id = ?",
                String.class,
                target.id()
        );

        int expectedRemaining;
        int expectedRestorations;

        if ("PAID".equals(finalStatus)) {
            assertEquals(4, paidSucceeded);
            assertEquals(0, cancelledSucceeded);
            expectedRemaining = 8;
            expectedRestorations = 0;
        } else {
            assertEquals("CANCELLED", finalStatus);
            assertEquals(0, paidSucceeded);
            assertEquals(4, cancelledSucceeded);
            expectedRemaining = 9;
            expectedRestorations = 1;
        }

        assertEquals(4, conflicts);
        assertInventoryAndOrderCount(tripId, expectedRemaining, 2);

        verify(ticketInventoryMapper, times(expectedRestorations))
                .restoreOneIfNotFull(tripId);

        // The unrelated reservation must remain completely unchanged.
        assertEquals(retainedBefore, jdbcTemplate.queryForMap(
                "SELECT * FROM ticket_orders WHERE id = ?",
                retained.id()
        ));

        mockMvc.perform(get("/api/orders/{id}", target.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(finalStatus));

        mockMvc.perform(get("/api/trains/{id}/inventory", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingTickets")
                        .value(expectedRemaining));
    }

    @Test
    void shouldPaginateOrdersNewestFirst() throws Exception {
        MockHttpSession session = registerAndLogin();
        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Page Rider",
                        "00000000000000021X",
                        "13800000021"
                )
        );

        // Use isolated test data without changing existing orders.
        Long tripId = createTrip();
        createInventory(tripId, 3, 3);

        OrderCreateResponse first =
                createOrder(session, tripId, passenger.id());
        OrderCreateResponse second =
                createOrder(session, tripId, passenger.id());
        OrderCreateResponse third =
                createOrder(session, tripId, passenger.id());

        assertTrue(third.id() > second.id());
        assertTrue(second.id() > first.id());

        // Check the first page, partial last page, and out-of-range page.
        List<List<Long>> expectedPages = List.of(
                List.of(third.id(), second.id()),
                List.of(first.id()),
                List.of()
        );

        for (int page = 1; page <= expectedPages.size(); page++) {
            MvcResult result = mockMvc.perform(get("/api/orders")
                            .session(session)
                            .param("page", Integer.toString(page))
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(page))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.total").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andReturn();

            OrderPageResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsByteArray(),
                    OrderPageResponse.class
            );

            List<Long> actualIds = response.items().stream()
                    .map(item -> item.id())
                    .toList();

            assertEquals(expectedPages.get(page - 1), actualIds);
        }

        // A large valid page must not overflow the offset calculation.
        mockMvc.perform(get("/api/orders")
                        .session(session)
                        .param("page", Integer.toString(Integer.MAX_VALUE))
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.page").value(Integer.MAX_VALUE))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));

        // Reading pages must not change orders or inventory.
        assertInventoryAndOrderCount(tripId, 0, 3);
    }

    @Test
    void shouldRejectInvalidPaginationParameters() throws Exception {
        MockHttpSession session = registerAndLogin();

        String[][] invalidParameters = {
                {"page", "0"},
                {"page", "-1"},
                {"page", "1.5"},
                {"page", "2147483648"},
                {"page", "PRIVATE_PAGE_MARKER"},
                {"size", "0"},
                {"size", "-1"},
                {"size", "101"},
                {"size", "1.5"},
                {"size", "2147483648"},
                {"size", "PRIVATE_SIZE_MARKER"}
        };

        for (String[] parameter : invalidParameters) {
            // Keep authentication valid to isolate parameter failures.
            MvcResult result = mockMvc.perform(get("/api/orders")
                            .session(session)
                            .param(parameter[0], parameter[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(
                            APPLICATION_JSON
                    ))
                    .andExpect(jsonPath("$.message").isString())
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andReturn();

            // Conversion errors must not echo submitted input.
            String body = result.getResponse().getContentAsString();
            assertFalse(body.contains("PRIVATE_PAGE_MARKER"));
            assertFalse(body.contains("PRIVATE_SIZE_MARKER"));
        }
    }

    private MockHttpSession registerAndLogin() throws Exception {
        String username = "passenger_" + UUID.randomUUID()
                .toString().replace("-", "");
        String credentials = objectMapper.writeValueAsString(
                new Credentials(username, "Train123"));

        // Obtain a session through the real authentication flow.
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

        // Lists are ordered by generated ID; the newest record is last.
        assertTrue(passengers.length > 0);
        return passengers[passengers.length - 1];
    }

    private Long createTrip() {
        // Keep test trips separate from the existing search seed data.
        String trainNumber = "T"
                + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12);

        jdbcTemplate.update("""
                INSERT INTO train_trips (
                    train_number, departure_station, arrival_station,
                    departure_at, arrival_at
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                trainNumber, "测试起点", "测试终点",
                "2099-01-01 08:00:00", "2099-01-01 09:00:00"
        );

        return jdbcTemplate.queryForObject("""
                SELECT id
                FROM train_trips
                WHERE train_number = ?
                  AND departure_at = ?
                """,
                Long.class, trainNumber, "2099-01-01 08:00:00"
        );
    }

    private void createInventory(
            Long tripId, int totalTickets, int remainingTickets
    ) {
        jdbcTemplate.update("""
                INSERT INTO ticket_inventory (
                    train_trip_id, total_tickets, remaining_tickets
                )
                VALUES (?, ?, ?)
                """,
                tripId, totalTickets, remainingTickets
        );
    }

    private void assertInventoryAndOrderCount(
            Long tripId, int expectedRemaining, long expectedOrders
    ) {
        Integer remaining = jdbcTemplate.queryForObject("""
            SELECT remaining_tickets
            FROM ticket_inventory
            WHERE train_trip_id = ?
            """, Integer.class, tripId);

        assertNotNull(remaining);
        assertEquals(expectedRemaining, remaining.intValue());

        Long count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM ticket_orders
            WHERE train_trip_id = ?
            """, Long.class, tripId);

        assertNotNull(count);
        assertEquals(expectedOrders, count.longValue());
    }

    private OrderCreateResponse createOrder(
            MockHttpSession session,
            Long tripId,
            Long passengerId
    ) throws Exception {
        // Create orders through the real HTTP flow.
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderCreateRequest(tripId, passengerId)
                        )))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                OrderCreateResponse.class
        );
    }

    private record Credentials(String username, String password) {

    }
}
