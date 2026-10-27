package com.mia.mini12306.train;

import com.mia.mini12306.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class TrainTripIntegrationTests extends DatabaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldFindTripsByDateAndRoute() throws Exception {
        mockMvc.perform(get("/api/trains")
                        .param("departureDate", "2026-11-01")
                        .param("fromStation", "上海虹桥")
                        .param("toStation", "杭州东"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].trainNumber").value("G101"))
                .andExpect(jsonPath("$[1].trainNumber").value("G103"));
    }

    @Test
    void shouldReturnEmptyListWhenNoTripsMatch() throws Exception {
        mockMvc.perform(get("/api/trains")
                        .param("departureDate", "2026-11-01")
                        .param("fromStation", "杭州东")
                        .param("toStation", "上海虹桥"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldRejectInvalidSearchParameters() throws Exception {
        mockMvc.perform(get("/api/trains")
                        .param("departureDate", "not-a-date")
                        .param("fromStation", "上海虹桥")
                        .param("toStation", "杭州东"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/trains")
                        .param("departureDate", "2026-11-01")
                        .param("toStation", "杭州东"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectSameDepartureAndArrivalStation() throws Exception {
        mockMvc.perform(get("/api/trains")
                        .param("departureDate", "2026-11-01")
                        .param("fromStation", "上海虹桥")
                        .param("toStation", "上海虹桥"))
                .andExpect(status().isBadRequest())
                // Return the controlled business message.
                .andExpect(jsonPath("$.message").value("起点和终点不能相同"));
    }
}
