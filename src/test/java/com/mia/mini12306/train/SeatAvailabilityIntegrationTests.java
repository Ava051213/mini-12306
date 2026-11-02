package com.mia.mini12306.train;

import com.mia.mini12306.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SeatAvailabilityIntegrationTests extends DatabaseIntegrationTest {

    private static final String QUERY = "/api/ticket-service/ticket/query";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRequireEverySegmentOfTheInterval() throws Exception {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 7, 1, 8, 0));
        List<Long> seats = insertSeats(trip.tripId(), 3);

        insertPrice(trip.tripId(), 1, 3, 2, 3000);
        insertPrice(trip.tripId(), 1, 2, 2, 1000);
        insertPrice(trip.tripId(), 2, 3, 2, 2000);

        assertQuantity(trip, "2027-07-01", 0, 2, 3);

        // One occupied segment takes the seat out of the whole route.
        occupySegment(seats.get(0), 2);

        assertQuantity(trip, "2027-07-01", 0, 2, 2);
        // The first interval never touches the occupied segment.
        assertQuantity(trip, "2027-07-01", 0, 1, 3);
        assertQuantity(trip, "2027-07-01", 1, 2, 2);
    }

    @Test
    void shouldReuseSeatAcrossAdjacentIntervals() throws Exception {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 7, 2, 8, 0));
        List<Long> seats = insertSeats(trip.tripId(), 3);

        insertPrice(trip.tripId(), 1, 2, 2, 1000);
        insertPrice(trip.tripId(), 2, 3, 2, 2000);
        insertPrice(trip.tripId(), 1, 3, 2, 3000);

        // Sold on the first segment only.
        occupySegment(seats.get(0), 1);

        assertQuantity(trip, "2027-07-02", 0, 1, 2);
        assertQuantity(trip, "2027-07-02", 1, 2, 3);
        assertQuantity(trip, "2027-07-02", 0, 2, 2);
    }

    @Test
    void shouldNotSellSeatWhoseSegmentRowIsMissing() throws Exception {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 7, 3, 8, 0));
        List<Long> seats = insertSeats(trip.tripId(), 3);

        insertPrice(trip.tripId(), 1, 3, 2, 3000);

        // An incomplete seat must count as unsellable, never as free.
        deleteSegment(seats.get(0), 2);

        assertQuantity(trip, "2027-07-03", 0, 2, 2);
    }

    @Test
    void shouldCountEachSeatTypeSeparately() throws Exception {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 7, 4, 8, 0));

        Long carriageId = insertCarriage(trip.tripId(), "01");
        insertSeatWithSegments(trip.tripId(), carriageId, "01A", 2);
        insertSeatWithSegments(trip.tripId(), carriageId, "01B", 2);
        insertSeatWithSegments(trip.tripId(), carriageId, "01C", 1);

        // Inserted out of order so the order comes from SQL.
        insertPrice(trip.tripId(), 1, 3, 2, 3000);
        insertPrice(trip.tripId(), 1, 3, 1, 8800);

        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2027-07-04")
                        .param("fromStation", trip.codes().get(0))
                        .param("toStation", trip.codes().get(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList.length()")
                        .value(2))
                .andExpect(jsonPath("$.trainList[0].seatClassList[0].type")
                        .value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList[0].quantity")
                        .value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList[1].type")
                        .value(2))
                .andExpect(jsonPath("$.trainList[0].seatClassList[1].quantity")
                        .value(2));
    }

    @Test
    void shouldReturnZeroWhenNoSeatIsConfigured() throws Exception {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 7, 5, 8, 0));

        insertPrice(trip.tripId(), 1, 3, 2, 3000);

        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2027-07-05")
                        .param("fromStation", trip.codes().get(0))
                        .param("toStation", trip.codes().get(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList.length()")
                        .value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList[0].quantity")
                        .value(0));
    }

    @Test
    void shouldRejectSegmentRowsThatBreakTheModel() {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 7, 6, 8, 0));

        Long carriageId = insertCarriage(trip.tripId(), "01");
        Long seatId = insertSeat(trip.tripId(), carriageId, "01A", 2);
        insertSegment(trip.tripId(), seatId, 1);

        // One status per seat and segment.
        DataAccessException duplicate = assertThrows(
                DataAccessException.class,
                () -> insertSegment(trip.tripId(), seatId, 1));
        assertTrue(duplicate.getMessage().contains("uk_train_seat_segments"));

        DataAccessException status = assertThrows(
                DataAccessException.class,
                () -> insertSegment(trip.tripId(), seatId, 2, "LOCKED"));
        assertTrue(status.getMessage()
                .contains("chk_train_seat_segments_status"));

        // The segment start must be a real stop of this run.
        DataAccessException stop = assertThrows(
                DataAccessException.class,
                () -> insertSegment(trip.tripId(), seatId, 9));
        assertTrue(stop.getMessage()
                .contains("fk_train_seat_segments_stop"));

        DataAccessException seatType = assertThrows(
                DataAccessException.class,
                () -> insertSeat(trip.tripId(), carriageId, "01B", 3));
        assertTrue(seatType.getMessage().contains("chk_train_seats_seat_type"));
    }

    @Test
    void shouldRejectSeatBoundToAnotherTripCarriage() {
        LocalDateTime departure = LocalDateTime.of(2027, 7, 7, 8, 0);
        ThreeStopTrip first = createThreeStopTrip(departure);
        ThreeStopTrip second = createThreeStopTrip(departure.plusDays(1));

        Long carriageOfFirst = insertCarriage(first.tripId(), "01");

        // The composite foreign key blocks another run's carriage.
        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> insertSeat(second.tripId(), carriageOfFirst, "01A", 2));
        assertTrue(exception.getMessage().contains("fk_train_seats_carriage"));
    }

    private record ThreeStopTrip(
            String trainNumber,
            Long tripId,
            List<String> codes
    ) {
    }

    // Two segments per seat: [1,2) and [2,3).
    private ThreeStopTrip createThreeStopTrip(LocalDateTime firstDeparture) {
        String suffix = uniqueSuffix();
        List<String> names = List.of(
                "甲站" + suffix, "乙站" + suffix, "丙站" + suffix);
        List<String> codes = List.of(
                "A" + suffix, "B" + suffix, "C" + suffix);

        List<Long> stationIds = new ArrayList<>();
        for(int i = 0; i < codes.size(); i++) {
            stationIds.add(insertStation(codes.get(i), names.get(i)));
        }

        Long tripId = insertTrip(
                "T" + suffix, names.get(0), names.get(2),
                firstDeparture, firstDeparture.plusHours(2));

        insertStop(tripId, stationIds.get(0), 1, null, firstDeparture);
        insertStop(tripId, stationIds.get(1), 2,
                firstDeparture.plusHours(1),
                firstDeparture.plusHours(1).plusMinutes(5));
        insertStop(tripId, stationIds.get(2), 3,
                firstDeparture.plusHours(2), null);

        return new ThreeStopTrip("T" + suffix, tripId, codes);
    }

    private List<Long> insertSeats(Long trainTripId, int count) {
        Long carriageId = insertCarriage(trainTripId, "01");
        List<Long> seatIds = new ArrayList<>();

        for(int i = 0; i < count; i++) {
            seatIds.add(insertSeatWithSegments(
                    trainTripId, carriageId, "0" + (i + 1) + "A", 2));
        }

        return seatIds;
    }

    private Long insertSeatWithSegments(
            Long trainTripId,
            Long carriageId,
            String seatNumber,
            int seatType
    ) {
        Long seatId = insertSeat(trainTripId, carriageId, seatNumber, seatType);

        // Every seat gets one row per adjacent segment.
        for(int sequence = 1; sequence < 3; sequence++) {
            insertSegment(trainTripId, seatId, sequence);
        }

        return seatId;
    }

    private Long insertCarriage(Long trainTripId, String carriageNumber) {
        jdbcTemplate.update("""
                INSERT INTO train_carriages (train_trip_id, carriage_number)
                VALUES (?, ?)
                """, trainTripId, carriageNumber);

        return jdbcTemplate.queryForObject("""
                SELECT id FROM train_carriages
                WHERE train_trip_id = ? AND carriage_number = ?
                """, Long.class, trainTripId, carriageNumber);
    }

    private Long insertSeat(
            Long trainTripId,
            Long carriageId,
            String seatNumber,
            int seatType
    ) {
        jdbcTemplate.update("""
                INSERT INTO train_seats
                    (train_trip_id, carriage_id, seat_number, seat_type)
                VALUES (?, ?, ?, ?)
                """, trainTripId, carriageId, seatNumber, seatType);

        return jdbcTemplate.queryForObject("""
                SELECT id FROM train_seats
                WHERE train_trip_id = ? AND carriage_id = ? AND seat_number = ?
                """, Long.class, trainTripId, carriageId, seatNumber);
    }

    private void insertSegment(
            Long trainTripId,
            Long seatId,
            int segmentSequence
    ) {
        insertSegment(trainTripId, seatId, segmentSequence, "AVAILABLE");
    }

    private void insertSegment(
            Long trainTripId,
            Long seatId,
            int segmentSequence,
            String status
    ) {
        jdbcTemplate.update("""
                INSERT INTO train_seat_segments
                    (train_trip_id, seat_id, segment_sequence, seat_status)
                VALUES (?, ?, ?, ?)
                """, trainTripId, seatId, segmentSequence, status);
    }

    private void occupySegment(Long seatId, int segmentSequence) {
        jdbcTemplate.update("""
                UPDATE train_seat_segments
                SET seat_status = 'OCCUPIED'
                WHERE seat_id = ? AND segment_sequence = ?
                """, seatId, segmentSequence);
    }

    private void deleteSegment(Long seatId, int segmentSequence) {
        jdbcTemplate.update("""
                DELETE FROM train_seat_segments
                WHERE seat_id = ? AND segment_sequence = ?
                """, seatId, segmentSequence);
    }

    private void assertQuantity(
            ThreeStopTrip trip,
            String departureDate,
            int fromIndex,
            int toIndex,
            int expected
    ) throws Exception {
        mockMvc.perform(get(QUERY)
                        .param("departureDate", departureDate)
                        .param("fromStation", trip.codes().get(fromIndex))
                        .param("toStation", trip.codes().get(toIndex)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList.length()")
                        .value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList[0].quantity")
                        .value(expected));
    }

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime(), 36).toUpperCase();
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
