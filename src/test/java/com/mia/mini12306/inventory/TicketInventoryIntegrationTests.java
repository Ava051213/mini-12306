package com.mia.mini12306.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketInventoryIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnRemainingTicketsWithoutLogin() throws Exception {
        Long tripId = createTrip();
        createInventory(tripId, 20, 7);

        mockMvc.perform(get("/api/trains/{id}/inventory", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainTripId").value(tripId))
                .andExpect(jsonPath("$.remainingTickets").value(7))
                .andExpect(jsonPath("$.totalTickets").doesNotExist())
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNull(
                        result.getRequest().getSession(false)
                ));
    }

    @Test
    void shouldReturnZeroWhenSoldOut() throws Exception {
        Long tripId = createTrip();
        createInventory(tripId, 20, 0);

        mockMvc.perform(get("/api/trains/{id}/inventory", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingTickets").value(0));
    }

    @Test
    void shouldReturnNotFoundWhenInventoryIsMissing() throws Exception {
        // An existing trip may not have inventory configured.
        Long tripId = createTrip();

        mockMvc.perform(get("/api/trains/{id}/inventory", tripId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("车次不存在或未配置库存"));

        mockMvc.perform(get("/api/trains/{id}/inventory", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("车次不存在或未配置库存"));
    }

    @Test
    void shouldRejectInvalidTripIds() throws Exception {
        for (String id : new String[]{"0", "-1", "not-a-number"}) {
            mockMvc.perform(get("/api/trains/{id}/inventory", id))
                    .andExpect(status().isBadRequest());
        }
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
}
