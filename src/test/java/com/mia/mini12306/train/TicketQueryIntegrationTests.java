package com.mia.mini12306.train;

import com.mia.mini12306.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TicketQueryIntegrationTests extends DatabaseIntegrationTest {

    private static final String QUERY = "/api/ticket-service/ticket/query";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldMatchOriginAdjacentAndTerminusIntervals() throws Exception {
        CrossDayTrip trip = createCrossDayTrip();

        // Origin to terminus keeps the real boarding and alighting times.
        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-20")
                        .param("fromStation", trip.codes().get(0))
                        .param("toStation", trip.codes().get(3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(1))
                .andExpect(jsonPath("$.trainList[0].trainId").value(trip.tripId().toString()))
                .andExpect(jsonPath("$.trainList[0].trainNumber").value(trip.trainNumber()))
                .andExpect(jsonPath("$.trainList[0].departure").value(trip.names().get(0)))
                .andExpect(jsonPath("$.trainList[0].arrival").value(trip.names().get(3)))
                .andExpect(jsonPath("$.trainList[0].departureTime").value("2026-12-20T23:30:00"))
                .andExpect(jsonPath("$.trainList[0].arrivalTime").value("2026-12-21T02:00:00"))
                // Seats and prices belong to later modules and must not appear.
                .andExpect(jsonPath("$.trainList[0].seatClassList").doesNotExist());

        // Adjacent stops in the forward direction are sellable.
        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-20")
                        .param("fromStation", trip.codes().get(0))
                        .param("toStation", trip.codes().get(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(1))
                .andExpect(jsonPath("$.trainList[0].arrivalTime").value("2026-12-21T00:20:00"));

        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-21")
                        .param("fromStation", trip.codes().get(2))
                        .param("toStation", trip.codes().get(3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(1))
                .andExpect(jsonPath("$.trainList[0].departureTime").value("2026-12-21T01:15:00"));
    }

    @Test
    void shouldMatchMiddleStopsAndKeepCrossDayTimes() throws Exception {
        CrossDayTrip trip = createCrossDayTrip();

        // Boarding at a middle stop uses that stop's own departure time.
        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-21")
                        .param("fromStation", trip.codes().get(1))
                        .param("toStation", trip.codes().get(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(1))
                .andExpect(jsonPath("$.trainList[0].departureTime").value("2026-12-21T00:25:00"))
                .andExpect(jsonPath("$.trainList[0].arrivalTime").value("2026-12-21T01:10:00"));
    }

    @Test
    void shouldRejectReverseDirectionButAllowSameStationAsClientError() throws Exception {
        CrossDayTrip trip = createCrossDayTrip();

        // A later stop cannot board towards an earlier stop.
        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-21")
                        .param("fromStation", trip.codes().get(2))
                        .param("toStation", trip.codes().get(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(0));

        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-20")
                        .param("fromStation", trip.codes().get(0))
                        .param("toStation", trip.codes().get(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("起点和终点不能相同"));
    }

    @Test
    void shouldFilterByBoardingDateRatherThanOriginDate() throws Exception {
        CrossDayTrip trip = createCrossDayTrip();

        // The middle stop departs on 12-21, so 12-20 must not match it.
        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-20")
                        .param("fromStation", trip.codes().get(1))
                        .param("toStation", trip.codes().get(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(0));

        // The origin departs on 12-20, so 12-21 must not match it either.
        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-21")
                        .param("fromStation", trip.codes().get(0))
                        .param("toStation", trip.codes().get(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(0));
    }

    @Test
    void shouldOrderResultsByBoardingTimeThenTrainNumber() throws Exception {
        String suffix = uniqueSuffix();
        String fromCode = "M" + suffix;
        String toCode = "N" + suffix;
        String fromName = "排序甲站" + suffix;
        String toName = "排序乙站" + suffix;

        Long fromId = insertStation(fromCode, fromName, "paixu", "测试地区");
        Long toId = insertStation(toCode, toName, "paixu", "测试地区");

        // Insert the latest trip first so ordering cannot come from insert order.
        insertSimpleTrip("9" + suffix, fromId, toId, fromName, toName, 9, 0);
        insertSimpleTrip("7" + suffix, fromId, toId, fromName, toName, 8, 0);
        insertSimpleTrip("3" + suffix, fromId, toId, fromName, toName, 8, 0);

        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2027-01-15")
                        .param("fromStation", fromCode)
                        .param("toStation", toCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(3))
                .andExpect(jsonPath("$.trainList[0].trainNumber").value("3" + suffix))
                .andExpect(jsonPath("$.trainList[1].trainNumber").value("7" + suffix))
                .andExpect(jsonPath("$.trainList[2].trainNumber").value("9" + suffix));
    }

    @Test
    void shouldReturnEmptyListForLegalStationsWithoutService() throws Exception {
        String suffix = uniqueSuffix();
        String fromCode = "P" + suffix;
        String toCode = "Q" + suffix;
        insertStation(fromCode, "孤立甲站" + suffix, "guli", "测试地区");
        insertStation(toCode, "孤立乙站" + suffix, "guli", "测试地区");

        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-20")
                        .param("fromStation", fromCode)
                        .param("toStation", toCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(0));
    }

    @Test
    void shouldRejectUnknownStationCode() throws Exception {
        String suffix = uniqueSuffix();
        String knownCode = "R" + suffix;
        insertStation(knownCode, "已知站" + suffix, "yizhi", "测试地区");

        // One unknown endpoint is enough to reject the request.
        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-20")
                        .param("fromStation", knownCode)
                        .param("toStation", "X" + suffix))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("出发地或目的地不存在"));

        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-20")
                        .param("fromStation", "X" + suffix)
                        .param("toStation", "Y" + suffix))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("出发地或目的地不存在"));
    }

    @Test
    void shouldRejectMissingOrMalformedParameters() throws Exception {
        mockMvc.perform(get(QUERY)
                        .param("fromStation", "AAA")
                        .param("toStation", "BBB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get(QUERY)
                        .param("departureDate", "not-a-date")
                        .param("fromStation", "AAA")
                        .param("toStation", "BBB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                // Never echo the raw rejected input.
                .andExpect(content().string(not(containsString("not-a-date"))));

        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-20")
                        .param("toStation", "BBB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldNotCreateSessionForAnonymousQuery() throws Exception {
        CrossDayTrip trip = createCrossDayTrip();

        MvcResult result = mockMvc.perform(get(QUERY)
                        .param("departureDate", "2026-12-20")
                        .param("fromStation", trip.codes().get(0))
                        .param("toStation", trip.codes().get(3)))
                .andExpect(status().isOk())
                .andReturn();

        assertNull(result.getRequest().getSession(false));
    }

    private record CrossDayTrip(
            Long tripId,
            String trainNumber,
            List<String> codes,
            List<String> names
    ) {
    }

    // Four stops crossing midnight: origin 12-20 23:30, terminus 12-21 02:00.
    private CrossDayTrip createCrossDayTrip() {
        String suffix = uniqueSuffix();
        List<String> names = List.of(
                "甲站" + suffix, "乙站" + suffix,
                "丙站" + suffix, "丁站" + suffix);
        String fromCode = "A" + suffix;
        String toCode = "B" + suffix;
        String middleCode = "C" + suffix;
        String lastCode = "D" + suffix;
        List<String> codes = List.of(fromCode, toCode, middleCode, lastCode);

        Long fromId = insertStation(fromCode, names.get(0), "jia", "测试地区");
        Long toId = insertStation(toCode, names.get(1), "yi", "测试地区");
        Long middleId = insertStation(middleCode, names.get(2), "bing", "测试地区");
        Long lastId = insertStation(lastCode, names.get(3), "ding", "测试地区");

        String trainNumber = "X" + suffix;
        Long tripId = insertTrip(trainNumber, names.get(0), names.get(3),
                LocalDateTime.of(2026, 12, 20, 23, 30),
                LocalDateTime.of(2026, 12, 21, 2, 0));

        insertStop(tripId, fromId, 1, null,
                LocalDateTime.of(2026, 12, 20, 23, 30));
        insertStop(tripId, toId, 2,
                LocalDateTime.of(2026, 12, 21, 0, 20),
                LocalDateTime.of(2026, 12, 21, 0, 25));
        insertStop(tripId, middleId, 3,
                LocalDateTime.of(2026, 12, 21, 1, 10),
                LocalDateTime.of(2026, 12, 21, 1, 15));
        insertStop(tripId, lastId, 4,
                LocalDateTime.of(2026, 12, 21, 2, 0), null);

        return new CrossDayTrip(tripId, trainNumber, codes, names);
    }

    private void insertSimpleTrip(
            String trainNumber, Long fromId, Long toId,
            String fromName, String toName, int hour, int minute
    ) {
        LocalDateTime departureAt = LocalDateTime.of(2027, 1, 15, hour, minute);
        Long tripId = insertTrip(trainNumber, fromName, toName,
                departureAt, departureAt.plusHours(1));

        insertStop(tripId, fromId, 1, null, departureAt);
        insertStop(tripId, toId, 2, departureAt.plusHours(1), null);
    }

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime(), 36).toUpperCase();
    }

    private Long insertStation(
            String code, String name, String spell, String regionName
    ) {
        jdbcTemplate.update("""
                INSERT INTO stations (code, name, spell, region_name)
                VALUES (?, ?, ?, ?)
                """, code, name, spell, regionName);

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
                INSERT INTO train_trips
                    (train_number, departure_station, arrival_station,
                     departure_at, arrival_at)
                VALUES (?, ?, ?, ?, ?)
                """, trainNumber, fromStation, toStation,
                departureAt, arrivalAt);

        return jdbcTemplate.queryForObject("""
                SELECT id FROM train_trips
                WHERE train_number = ? AND departure_at = ?
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
                INSERT INTO train_trip_stops
                    (train_trip_id, station_id, stop_sequence,
                     arrival_at, departure_at)
                VALUES (?, ?, ?, ?, ?)
                """, trainTripId, stationId, stopSequence,
                arrivalAt, departureAt);
    }
}
