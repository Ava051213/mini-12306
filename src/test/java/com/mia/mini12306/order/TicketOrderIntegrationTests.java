package com.mia.mini12306.order;

import com.mia.mini12306.inventory.InsufficientTicketsException;
import com.mia.mini12306.order.dto.OrderCreateRequest;
import com.mia.mini12306.order.dto.OrderCreateResponse;
import com.mia.mini12306.order.mapper.TicketOrderMapper;
import com.mia.mini12306.passenger.dto.PassengerResponse;
import com.mia.mini12306.passenger.dto.PassengerSaveRequest;
import com.mia.mini12306.user.CurrentUserSession;
import org.junit.jupiter.api.Test;
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
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
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

    private record Credentials(String username, String password) {

    }
}
