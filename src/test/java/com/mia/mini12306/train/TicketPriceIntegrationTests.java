package com.mia.mini12306.train;

import com.mia.mini12306.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TicketPriceIntegrationTests extends DatabaseIntegrationTest {

    private static final String QUERY = "/api/ticket-service/ticket/query";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnAllSeatTypesInCodeOrderWithTwoDecimals() throws Exception {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 3, 1, 8, 0));

        // Insert out of order so the order can only come from SQL.
        insertPrice(trip.tripId(), 1, 3, 2, 3000);
        insertPrice(trip.tripId(), 1, 3, 0, 12345);
        insertPrice(trip.tripId(), 1, 3, 1, 5678);

        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2027-03-01")
                        .param("fromStation", trip.codes().get(0))
                        .param("toStation", trip.codes().get(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList.length()").value(3))
                .andExpect(jsonPath("$.trainList[0].seatClassList[0].type").value(0))
                .andExpect(jsonPath("$.trainList[0].seatClassList[1].type").value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList[2].type").value(2))
                // One decimal would print 123.5, 56.8 and 30.0 here.
                .andExpect(content().string(containsString("\"price\":123.45")))
                .andExpect(content().string(containsString("\"price\":56.78")))
                .andExpect(content().string(containsString("\"price\":30.00")));
    }

    @Test
    void shouldKeepPricesOfDifferentIntervalsApart() throws Exception {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 3, 2, 8, 0));

        insertPrice(trip.tripId(), 1, 2, 2, 1000);
        insertPrice(trip.tripId(), 1, 3, 2, 3000);
        insertPrice(trip.tripId(), 2, 3, 2, 2000);

        assertSinglePrice(trip, 0, 1, "2027-03-02", "10.00");
        assertSinglePrice(trip, 1, 2, "2027-03-02", "20.00");
        assertSinglePrice(trip, 0, 2, "2027-03-02", "30.00");
    }

    @Test
    void shouldKeepInstancesOfTheSameTrainNumberApart() throws Exception {
        String suffix = uniqueSuffix();
        String trainNumber = "T" + suffix;

        ThreeStopTrip first = createThreeStopTrip(
                trainNumber, suffix + "A",
                LocalDateTime.of(2027, 3, 3, 8, 0));
        ThreeStopTrip second = createThreeStopTrip(
                trainNumber, suffix + "B",
                LocalDateTime.of(2027, 3, 4, 8, 0));

        insertPrice(first.tripId(), 1, 3, 2, 1111);
        insertPrice(second.tripId(), 1, 3, 2, 2222);

        // The price follows the run, not the train number.
        assertSinglePrice(first, 0, 2, "2027-03-03", "11.11");
        assertSinglePrice(second, 0, 2, "2027-03-04", "22.22");
    }

    @Test
    void shouldKeepTripWithoutPricesWithEmptySeatClassList() throws Exception {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 3, 5, 8, 0));

        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2027-03-05")
                        .param("fromStation", trip.codes().get(0))
                        .param("toStation", trip.codes().get(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList").isEmpty());
    }

    @Test
    void shouldExposeOnlyConfiguredSeatTypes() throws Exception {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 3, 6, 8, 0));

        insertPrice(trip.tripId(), 1, 3, 1, 8800);

        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2027-03-06")
                        .param("fromStation", trip.codes().get(0))
                        .param("toStation", trip.codes().get(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList[0].seatClassList.length()").value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList[0].type").value(1))
                // Quantity and candidate belong to later modules.
                .andExpect(jsonPath("$.trainList[0].seatClassList[0].quantity")
                        .doesNotExist())
                .andExpect(jsonPath("$.trainList[0].seatClassList[0].candidate")
                        .doesNotExist());
    }

    @Test
    void shouldRejectDuplicateSeatPrice() {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 3, 7, 8, 0));

        insertPrice(trip.tripId(), 1, 3, 2, 1000);

        DataAccessException exception = assertThrows(
                DataAccessException.class, () ->
                        insertPrice(trip.tripId(), 1, 3, 2, 2000));

        assertTrue(exception.getMessage()
                .contains("uk_train_trip_interval_prices"));
    }

    @Test
    void shouldRejectReverseOrSameStopInterval() {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 3, 8, 8, 0));

        DataAccessException reverse = assertThrows(
                DataAccessException.class, () ->
                        insertPrice(trip.tripId(), 3, 1, 2, 1000));

        assertTrue(reverse.getMessage()
                .contains("chk_train_trip_interval_prices_order"));

        DataAccessException sameStop = assertThrows(
                DataAccessException.class, () ->
                        insertPrice(trip.tripId(), 2, 2, 2, 1000));

        assertTrue(sameStop.getMessage()
                .contains("chk_train_trip_interval_prices_order"));
    }

    @Test
    void shouldRejectSequenceMissingFromThisTrip() {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 3, 9, 8, 0));

        // The composite foreign key is what makes another trip's sequence unusable.
        DataAccessException exception = assertThrows(
                DataAccessException.class, () ->
                        insertPrice(trip.tripId(), 1, 5, 2, 1000));

        assertTrue(exception.getMessage()
                .contains("fk_train_trip_interval_prices_arrival"));
    }

    @Test
    void shouldRejectUnsupportedSeatTypeAndNonPositivePrice() {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 3, 10, 8, 0));

        DataAccessException seatType = assertThrows(
                DataAccessException.class, () ->
                        insertPrice(trip.tripId(), 1, 3, 3, 1000));

        assertTrue(seatType.getMessage()
                .contains("chk_train_trip_interval_prices_seat_type"));

        DataAccessException price = assertThrows(
                DataAccessException.class, () ->
                        insertPrice(trip.tripId(), 1, 3, 2, 0));

        assertTrue(price.getMessage()
                .contains("chk_train_trip_interval_prices_price"));
    }

    private record ThreeStopTrip(
            String trainNumber,
            Long tripId,
            List<String> codes,
            List<String> names
    ) {
    }

    // Three stops on one day: 08:00, 09:00 and 10:00.
    private ThreeStopTrip createThreeStopTrip(LocalDateTime firstDeparture) {
        String suffix = uniqueSuffix();

        return createThreeStopTrip("T" + suffix, suffix, firstDeparture);
    }

    private ThreeStopTrip createThreeStopTrip(
            String trainNumber,
            String suffix,
            LocalDateTime firstDeparture
    ) {
        List<String> names = List.of(
                "甲站" + suffix, "乙站" + suffix, "丙站" + suffix);
        List<String> codes = List.of(
                "A" + suffix, "B" + suffix, "C" + suffix);

        Long firstId = insertStation(codes.get(0), names.get(0), "jia", "测试地区");
        Long secondId = insertStation(codes.get(1), names.get(1), "yi", "测试地区");
        Long lastId = insertStation(codes.get(2), names.get(2), "bing", "测试地区");

        Long tripId = insertTrip(trainNumber, names.get(0), names.get(2),
                firstDeparture, firstDeparture.plusHours(2));

        insertStop(tripId, firstId, 1, null, firstDeparture);
        insertStop(tripId, secondId, 2,
                firstDeparture.plusHours(1),
                firstDeparture.plusHours(1).plusMinutes(5));
        insertStop(tripId, lastId, 3,
                firstDeparture.plusHours(2), null);

        return new ThreeStopTrip(trainNumber, tripId, codes, names);
    }

    private void assertSinglePrice(
            ThreeStopTrip trip,
            int fromIndex,
            int toIndex,
            String departureDate,
            String priceJson
    ) throws Exception {
        mockMvc.perform(get(QUERY)
                        .param("departureDate", departureDate)
                        .param("fromStation", trip.codes().get(fromIndex))
                        .param("toStation", trip.codes().get(toIndex)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList.length()").value(1))
                .andExpect(content().string(
                        containsString("\"price\":" + priceJson)));
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

    private void insertPrice(
            Long trainTripId,
            int departureSequence,
            int arrivalSequence,
            int seatType,
            int priceCents
    ) {
        jdbcTemplate.update("""
                INSERT INTO train_trip_interval_prices
                    (train_trip_id, departure_sequence, arrival_sequence,
                     seat_type, price_cents)
                VALUES (?, ?, ?, ?, ?)
                """, trainTripId, departureSequence, arrivalSequence,
                seatType, priceCents);
    }
}
