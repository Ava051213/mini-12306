package com.mia.mini12306.station;

import com.jayway.jsonpath.JsonPath;
import com.mia.mini12306.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class StationQueryIntegrationTests extends DatabaseIntegrationTest {

    private static final String STATION_LIST = "/api/ticket-service/station/all";

    private static final String STOP_QUERY =
            "/api/ticket-service/train-station/query";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldListStationsInCodeOrderWithDictionaryFields() throws Exception {
        String suffix = uniqueSuffix();
        String firstCode = "A" + suffix;
        String lastCode = "Z" + suffix;
        insertStation(lastCode, "字典末站" + suffix, "zidianmozhan", "测试地区");
        insertStation(firstCode, "字典首站" + suffix, "zidianshouzhan", "测试地区");

        String body = mockMvc.perform(get(STATION_LIST))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // The dictionary must already be sorted by the database, not by Java.
        List<String> codes = JsonPath.read(body, "$[*].code");
        List<String> expectedOrder = new ArrayList<>(codes);
        expectedOrder.sort(Comparator.naturalOrder());
        assertEquals(expectedOrder, codes);

        int index = codes.indexOf(firstCode);
        assertTrue(index >= 0);

        Map<String, Object> station = JsonPath.read(body, "$[" + index + "]");
        assertEquals("字典首站" + suffix, station.get("name"));
        assertEquals(firstCode, station.get("code"));
        assertEquals("zidianshouzhan", station.get("spell"));
        assertEquals("测试地区", station.get("regionName"));
        assertFalse(station.containsKey("id"));
    }

    @Test
    void shouldReturnStopsInNumericSequenceOrder() throws Exception {
        CrossDayTrip trip = createCrossDayTrip();

        mockMvc.perform(get(STOP_QUERY)
                        .param("trainId", trip.tripId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].sequence").value("1"))
                .andExpect(jsonPath("$[1].sequence").value("2"))
                .andExpect(jsonPath("$[2].sequence").value("10"))
                .andExpect(jsonPath("$[3].sequence").value("11"))
                .andExpect(jsonPath("$[0].departure")
                        .value("甲站" + trip.suffix()))
                .andExpect(jsonPath("$[2].departure")
                        .value("丙站" + trip.suffix()));
    }

    @Test
    void shouldKeepCrossDayTimesAndStopover() throws Exception {
        CrossDayTrip trip = createCrossDayTrip();

        String body = mockMvc.perform(get(STOP_QUERY)
                        .param("trainId", trip.tripId().toString()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> stops = JsonPath.read(body, "$[*]");

        // The first stop only departs, the last stop only arrives.
        assertNull(stops.get(0).get("arrivalTime"));
        assertEquals("2026-12-20T22:00:00", stops.get(0).get("departureTime"));
        assertNull(stops.get(0).get("stopoverTime"));

        // A middle stop keeps both times and the minutes in between.
        assertEquals("2026-12-20T23:10:00", stops.get(1).get("arrivalTime"));
        assertEquals("2026-12-20T23:15:00", stops.get(1).get("departureTime"));
        assertEquals(Integer.valueOf(5), stops.get(1).get("stopoverTime"));

        // Cross-day stops keep real dates instead of a bare clock time.
        assertEquals("2026-12-21T01:05:00", stops.get(2).get("arrivalTime"));
        assertEquals("2026-12-21T01:12:00", stops.get(2).get("departureTime"));
        assertEquals(Integer.valueOf(7), stops.get(2).get("stopoverTime"));

        assertEquals("2026-12-21T03:00:00", stops.get(3).get("arrivalTime"));
        assertNull(stops.get(3).get("departureTime"));
        assertNull(stops.get(3).get("stopoverTime"));
    }

    @Test
    void shouldReturnNotFoundForUnknownTrip() throws Exception {
        mockMvc.perform(get(STOP_QUERY).param("trainId", "9000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("车次不存在"));
    }

    @Test
    void shouldReturnEmptyListForTripWithoutStops() throws Exception {
        // Fixed reference trip from the test-only V2 migration, read only.
        Long tripId = jdbcTemplate.queryForObject("""
                SELECT id FROM train_trips
                WHERE train_number = 'G103'
                LIMIT 1
                """, Long.class);
        assertNotNull(tripId);

        mockMvc.perform(get(STOP_QUERY).param("trainId", tripId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldRejectMissingInvalidOrNonPositiveTrainId() throws Exception {
        mockMvc.perform(get(STOP_QUERY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get(STOP_QUERY).param("trainId", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(content().string(not(containsString("not-a-number"))));

        mockMvc.perform(get(STOP_QUERY).param("trainId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get(STOP_QUERY).param("trainId", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldNotCreateSessionForAnonymousQueries() throws Exception {
        MvcResult stations = mockMvc.perform(get(STATION_LIST))
                .andExpect(status().isOk())
                .andReturn();
        assertNull(stations.getRequest().getSession(false));

        CrossDayTrip trip = createCrossDayTrip();
        MvcResult stops = mockMvc.perform(get(STOP_QUERY)
                        .param("trainId", trip.tripId().toString()))
                .andExpect(status().isOk())
                .andReturn();
        assertNull(stops.getRequest().getSession(false));
    }

    @Test
    void shouldRejectDuplicateSequenceWithinTrip() {
        CrossDayTrip trip = createCrossDayTrip();

        assertThrows(DataIntegrityViolationException.class, () ->
                insertStop(trip.tripId(), trip.firstStationId(), 1,
                        null, LocalDateTime.of(2026, 12, 20, 22, 0)));
    }

    @Test
    void shouldRejectDuplicateStationCode() {
        String suffix = uniqueSuffix();
        String code = "C" + suffix;
        insertStation(code, "重复代码站" + suffix, "chongfu", "测试地区");

        assertThrows(DataIntegrityViolationException.class, () ->
                insertStation(code, "另一站" + suffix, "lingyizhan", "测试地区"));
    }

    @Test
    void shouldRejectStopWithUnknownStation() {
        CrossDayTrip trip = createCrossDayTrip();

        assertThrows(DataIntegrityViolationException.class, () ->
                insertStop(trip.tripId(), 9000000000L, 50,
                        null, LocalDateTime.of(2026, 12, 20, 22, 0)));
    }

    @Test
    void shouldRejectNonPositiveStopSequence() {
        CrossDayTrip trip = createCrossDayTrip();

        // MySQL reports CHECK violations as HY000/3819, which Spring cannot
        // classify as an integrity violation; assert the shared parent.
        DataAccessException exception = assertThrows(
                DataAccessException.class, () ->
                        insertStop(trip.tripId(), trip.firstStationId(), 0,
                                null,
                                LocalDateTime.of(2026, 12, 20, 22, 0)));

        assertTrue(exception.getMessage()
                .contains("chk_train_trip_stops_sequence"));
    }

    private record CrossDayTrip(
            Long tripId,
            String suffix,
            Long firstStationId
    ) {
    }


    // Four stops numbered 1, 2, 10, 11 and inserted out of order on purpose.
    private CrossDayTrip createCrossDayTrip() {
        String suffix = uniqueSuffix();
        String first = "甲站" + suffix;
        String second = "乙站" + suffix;
        String third = "丙站" + suffix;
        String last = "丁站" + suffix;

        Long firstStationId =
                insertStation("A" + suffix, first, "jia", "测试地区");
        Long secondStationId =
                insertStation("B" + suffix, second, "yi", "测试地区");
        Long thirdStationId =
                insertStation("C" + suffix, third, "bing", "测试地区");
        Long lastStationId =
                insertStation("D" + suffix, last, "ding", "测试地区");

        Long tripId = insertTrip("D" + suffix, first, last,
                LocalDateTime.of(2026, 12, 20, 22, 0),
                LocalDateTime.of(2026, 12, 21, 3, 0));

        // Insert out of order so the API proves SQL ordering, not insert order.
        insertStop(tripId, lastStationId, 11,
                LocalDateTime.of(2026, 12, 21, 3, 0), null);
        insertStop(tripId, secondStationId, 2,
                LocalDateTime.of(2026, 12, 20, 23, 10),
                LocalDateTime.of(2026, 12, 20, 23, 15));
        insertStop(tripId, thirdStationId, 10,
                LocalDateTime.of(2026, 12, 21, 1, 5),
                LocalDateTime.of(2026, 12, 21, 1, 12));
        insertStop(tripId, firstStationId, 1,
                null, LocalDateTime.of(2026, 12, 20, 22, 0));

        return new CrossDayTrip(tripId, suffix, firstStationId);
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
