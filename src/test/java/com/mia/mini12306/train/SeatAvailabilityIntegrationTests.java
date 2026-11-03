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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SeatAvailabilityIntegrationTests extends DatabaseIntegrationTest {

    private static final String QUERY = "/api/ticket-service/ticket/query";
    private static final String FIXTURE_ID_CARD = "000000000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // V9 requires every occupied segment to point at an order.
    private Long fixtureUserId;
    private Long fixturePassengerId;

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

        // A locked row breaks both the status list and the owner pairing,
        // so MySQL may report either segment check first.
        assertTrue(
                status.getMessage().contains("chk_train_seat_segments_status")
                        || status.getMessage()
                        .contains("chk_train_seat_segments_order")
        );

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

    @Test
    void shouldNotCombineDifferentSeatsIntoOneFullRoute() throws Exception {
        ThreeStopTrip trip = createThreeStopTrip(
                LocalDateTime.of(2027, 7, 8, 8, 0));
        List<Long> seats = insertSeats(trip.tripId(), 2);

        insertPrice(trip.tripId(), 1, 2, 2, 1000);
        insertPrice(trip.tripId(), 2, 3, 2, 2000);
        insertPrice(trip.tripId(), 1, 3, 2, 3000);

        // Seat A is free on the first segment only, seat B on the second only.
        occupySegment(seats.get(0), 2);
        occupySegment(seats.get(1), 1);

        assertQuantity(trip, "2027-07-08", 0, 1, 1);
        assertQuantity(trip, "2027-07-08", 1, 2, 1);
        // Each segment has a free seat, but no single seat covers both.
        assertQuantity(trip, "2027-07-08", 0, 2, 0);
    }

    @Test
    void shouldUnderCountWhenStopSequencesAreNotContinuous() throws Exception {
        String suffix = uniqueSuffix();
        LocalDateTime departure = LocalDateTime.of(2027, 7, 9, 8, 0);

        Long first = insertStation("A" + suffix, "甲站" + suffix);
        Long second = insertStation("B" + suffix, "乙站" + suffix);
        Long third = insertStation("C" + suffix, "丙站" + suffix);
        Long fourth = insertStation("D" + suffix, "丁站" + suffix);

        Long tripId = insertTrip("T" + suffix, "甲站" + suffix, "丁站" + suffix,
                departure, departure.plusHours(3));

        // Sequences 1, 2, 10, 11 leave a gap between 2 and 10.
        insertStop(tripId, first, 1, null, departure);
        insertStop(tripId, second, 2, departure.plusHours(1),
                departure.plusHours(1).plusMinutes(5));
        insertStop(tripId, third, 10, departure.plusHours(2),
                departure.plusHours(2).plusMinutes(5));
        insertStop(tripId, fourth, 11, departure.plusHours(3), null);

        Long carriageId = insertCarriage(tripId, "01");
        Long seatId = insertSeat(tripId, carriageId, "01A", 2);
        insertSegment(tripId, seatId, 1);
        insertSegment(tripId, seatId, 2);
        insertSegment(tripId, seatId, 10);

        insertPrice(tripId, 2, 10, 2, 1000);

        // 2 -> 10 is adjacent here, but the span still counts as 8 segments.
        mockMvc.perform(get(QUERY)
                        .param("departureDate", "2027-07-09")
                        .param("fromStation", "B" + suffix)
                        .param("toStation", "C" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainList.length()").value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList.length()")
                        .value(1))
                .andExpect(jsonPath("$.trainList[0].seatClassList[0].quantity")
                        .value(0));
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
        Long trainTripId = jdbcTemplate.queryForObject("""
                SELECT train_trip_id FROM train_seats WHERE id = ?
                """, Long.class, seatId);

        // Every occupied row needs an owner, so build one for the interval.
        Long orderId = createFixtureOrder(
                trainTripId, seatId, segmentSequence, segmentSequence + 1);

        jdbcTemplate.update("""
                UPDATE train_seat_segments
                SET seat_status = 'OCCUPIED',
                    order_id = ?
                WHERE seat_id = ? AND segment_sequence = ?
                """, orderId, seatId, segmentSequence);
    }

    private Long createFixtureOrder(
            Long trainTripId,
            Long seatId,
            int fromSequence,
            int toSequence
    ) {
        ensureFixtureOwner();

        Map<String, Object> seat = jdbcTemplate.queryForMap("""
                SELECT s.seat_number,
                       s.seat_type,
                       c.carriage_number
                FROM train_seats s
                JOIN train_carriages c
                  ON c.id = s.carriage_id
                 AND c.train_trip_id = s.train_trip_id
                WHERE s.id = ?
                """, seatId);

        Map<String, Object> boarding = stopRow(trainTripId, fromSequence);
        Map<String, Object> alighting = stopRow(trainTripId, toSequence);

        jdbcTemplate.update("""
                INSERT INTO ticket_orders (
                    user_id, train_trip_id, passenger_id,
                    passenger_name, passenger_id_card,
                    status, pay_deadline, purchase_mode,
                    departure_sequence, arrival_sequence,
                    departure_station_code, departure_station_name,
                    departure_at,
                    arrival_station_code, arrival_station_name,
                    arrival_at,
                    seat_type, amount_cents, seat_id,
                    carriage_number, seat_number
                )
                VALUES (
                    ?, ?, ?, ?, ?,
                    'PAID', DATE_ADD(NOW(), INTERVAL 30 MINUTE), 'SEAT',
                    ?, ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?, ?
                )
                """,
                fixtureUserId, trainTripId, fixturePassengerId,
                "Fixture Rider", FIXTURE_ID_CARD,
                fromSequence, toSequence,
                boarding.get("station_code"), boarding.get("station_name"),
                boarding.get("departure_at"),
                alighting.get("station_code"), alighting.get("station_name"),
                alighting.get("arrival_at"),
                seat.get("seat_type"), 1000, seatId,
                seat.get("carriage_number"), seat.get("seat_number"));

        return jdbcTemplate.queryForObject("""
                SELECT MAX(id)
                FROM ticket_orders
                WHERE train_trip_id = ?
                """, Long.class, trainTripId);
    }

    private Map<String, Object> stopRow(Long trainTripId, int sequence) {
        return jdbcTemplate.queryForMap("""
                SELECT st.code AS station_code,
                       st.name AS station_name,
                       s.arrival_at,
                       s.departure_at
                FROM train_trip_stops s
                JOIN stations st ON st.id = s.station_id
                WHERE s.train_trip_id = ?
                  AND s.stop_sequence = ?
                """, trainTripId, sequence);
    }

    private void ensureFixtureOwner() {
        if(fixtureUserId != null) {
            return;
        }

        String username = "seat_fixture_" + uniqueSuffix();
        jdbcTemplate.update("""
                INSERT INTO users (username, password_hash)
                VALUES (?, ?)
                """, username, "fixture");

        fixtureUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?",
                Long.class, username);

        jdbcTemplate.update("""
                INSERT INTO passengers (user_id, real_name, id_card, phone)
                VALUES (?, ?, ?, ?)
                """, fixtureUserId, "Fixture Rider",
                FIXTURE_ID_CARD, "13800000001");

        fixturePassengerId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM passengers
                WHERE user_id = ?
                  AND id_card = ?
                """, Long.class, fixtureUserId, FIXTURE_ID_CARD);
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
