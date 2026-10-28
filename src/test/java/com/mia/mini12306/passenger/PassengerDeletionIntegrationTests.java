package com.mia.mini12306.passenger;

import com.mia.mini12306.order.dto.OrderCreateRequest;
import com.mia.mini12306.order.dto.OrderCreateResponse;
import com.mia.mini12306.passenger.dto.PassengerResponse;
import com.mia.mini12306.passenger.dto.PassengerSaveRequest;
import com.mia.mini12306.support.DatabaseIntegrationTest;
import com.mia.mini12306.user.CurrentUserSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class PassengerDeletionIntegrationTests extends DatabaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldDeleteOwnPassengerAndHideItFromQueries() throws Exception {
        MockHttpSession session = registerAndLogin();

        PassengerResponse target = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Delete Rider", "00000000000000011X", "13800000011"
                )
        );
        PassengerResponse keeper = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Keep Rider", "00000000000000012X", "13800000012"
                )
        );

        mockMvc.perform(delete("/api/passengers/{id}", target.id())
                        .session(session))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // The deleted record must disappear from both detail and list queries.
        mockMvc.perform(get("/api/passengers/{id}", target.id())
                        .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("乘车人不存在"));

        MvcResult listResult = mockMvc.perform(get("/api/passengers")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(keeper.id()))
                .andReturn();

        PassengerResponse[] remaining = objectMapper.readValue(
                listResult.getResponse().getContentAsByteArray(),
                PassengerResponse[].class
        );
        assertArrayEquals(new PassengerResponse[]{keeper}, remaining);

        // Confirm the row is physically gone, not merely hidden by SQL filters.
        assertEquals(0L, countPassengerRows(target.id()));
        assertEquals(1L, countPassengerRows(keeper.id()));
    }

    @Test
    void shouldRejectDeletionWithoutLogin() throws Exception {
        MockHttpSession session = registerAndLogin();

        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Session Rider", "00000000000000013X", "13800000013"
                )
        );

        MvcResult result = mockMvc.perform(
                        delete("/api/passengers/{id}", passenger.id()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"))
                .andReturn();

        // A rejected request must not create a session.
        assertNull(result.getRequest().getSession(false));
        assertEquals(1L, countPassengerRows(passenger.id()));

        mockMvc.perform(get("/api/passengers/{id}", passenger.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passenger.id()));
    }

    @Test
    void shouldNotDeletePassengerOwnedByAnotherAccount() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        MockHttpSession otherSession = registerAndLogin();

        PassengerResponse target = createPassenger(
                ownerSession,
                new PassengerSaveRequest(
                        "Owner Rider", "00000000000000014X", "13800000014"
                )
        );

        // Foreign records must look exactly like missing ones.
        mockMvc.perform(delete("/api/passengers/{id}", target.id())
                        .session(otherSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("乘车人不存在"));

        MvcResult ownerResult = mockMvc.perform(
                        get("/api/passengers/{id}", target.id())
                                .session(ownerSession))
                .andExpect(status().isOk())
                .andReturn();

        PassengerResponse actual = objectMapper.readValue(
                ownerResult.getResponse().getContentAsByteArray(),
                PassengerResponse.class
        );
        assertEquals(target, actual);
        assertEquals(1L, countPassengerRows(target.id()));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingPassenger() throws Exception {
        MockHttpSession session = registerAndLogin();

        Long missingId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(id), 0) + 1 FROM passengers",
                Long.class
        );
        assertNotNull(missingId);

        mockMvc.perform(delete("/api/passengers/{id}", missingId)
                        .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("乘车人不存在"));

        assertEquals(0L, countPassengerRows(missingId));
    }

    @Test
    void shouldRejectRepeatedDeletion() throws Exception {
        MockHttpSession session = registerAndLogin();

        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Repeated Rider", "00000000000000015X", "13800000015"
                )
        );

        mockMvc.perform(delete("/api/passengers/{id}", passenger.id())
                        .session(session))
                .andExpect(status().isNoContent());

        // The second attempt affects no rows and reports the record as absent.
        mockMvc.perform(delete("/api/passengers/{id}", passenger.id())
                        .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("乘车人不存在"));

        assertEquals(0L, countPassengerRows(passenger.id()));
    }

    @Test
    void shouldRejectDeletionWhenPassengerIsReferencedByOrder() throws Exception {
        MockHttpSession session = registerAndLogin();

        PassengerResponse passenger = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Ordered Rider", "00000000000000016X", "13800000016"
                )
        );

        Long tripId = createTrip();
        createInventory(tripId, 5, 5);
        OrderCreateResponse order = createOrder(
                session, tripId, passenger.id()
        );

        mockMvc.perform(delete("/api/passengers/{id}", passenger.id())
                        .session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("该乘车人已被订单引用，无法删除"));

        // The refusal must leave both records untouched.
        MvcResult passengerResult = mockMvc.perform(
                        get("/api/passengers/{id}", passenger.id())
                                .session(session))
                .andExpect(status().isOk())
                .andReturn();

        PassengerResponse actual = objectMapper.readValue(
                passengerResult.getResponse().getContentAsByteArray(),
                PassengerResponse.class
        );
        assertEquals(passenger, actual);

        // The order keeps working and still shows its own snapshot.
        mockMvc.perform(get("/api/orders/{id}", order.id())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passengerId").value(passenger.id()))
                .andExpect(jsonPath("$.passengerName").value("Ordered Rider"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        assertEquals(1L, countPassengerRows(passenger.id()));

        // Stock must not move when the deletion is refused.
        Integer remaining = jdbcTemplate.queryForObject("""
                SELECT remaining_tickets
                FROM ticket_inventory
                WHERE train_trip_id = ?
                """, Integer.class, tripId);
        assertNotNull(remaining);
        assertEquals(4, remaining.intValue());
    }

    @Test
    void shouldRejectInvalidPassengerIdOnDeletion() throws Exception {
        MockHttpSession session = registerAndLogin();

        // A non-numeric ID fails conversion before the service is reached.
        mockMvc.perform(delete("/api/passengers/not-a-number")
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("请求参数类型或格式不正确"));

        // A numeric but absent ID is a business-level 404.
        mockMvc.perform(delete("/api/passengers/0").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("乘车人不存在"));
    }

    private MockHttpSession registerAndLogin() throws Exception {
        String username = "passenger_" + UUID.randomUUID()
                .toString().replace("-", "");
        String credentials = objectMapper.writeValueAsString(
                new Credentials(username, "Train123")
        );

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

    private OrderCreateResponse createOrder(
            MockHttpSession session,
            Long tripId,
            Long passengerId
    ) throws Exception {
        // Create orders through the real HTTP flow.
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
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

    private long countPassengerRows(Long passengerId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM passengers
                WHERE id = ?
                """, Long.class, passengerId);

        // A null count would hide a broken query behind an assertion failure.
        assertNotNull(count);
        return count;
    }

    private record Credentials(String username, String password) {

    }
}
