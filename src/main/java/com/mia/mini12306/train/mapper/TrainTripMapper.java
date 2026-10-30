package com.mia.mini12306.train.mapper;

import com.mia.mini12306.train.TrainTrip;
import com.mia.mini12306.train.TrainTripInterval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TrainTripMapper {

    // Use a half-open range so each departure belongs to one calendar day.
    @Select("""
            SELECT id,
                   train_number AS trainNumber,
                   departure_station AS departureStation,
                   arrival_station AS arrivalStation,
                   departure_at AS departureAt,
                   arrival_at AS arrivalAt
            FROM train_trips
            WHERE departure_station = #{fromStation}
                AND arrival_station = #{toStation}
                AND departure_at >= #{dayStart}
                AND departure_at < #{nextDayStart}
            ORDER BY departure_at
            """)
    List<TrainTrip> findByRouteAndDepartureDay(
            @Param("fromStation") String fromStation,
            @Param("toStation") String toStation,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("nextDayStart") LocalDateTime nextDayStart
    );

    // Distinguish a missing trip from a trip without stops.
    @Select("""
            SELECT COUNT(*) FROM train_trips WHERE id = #{id}
            """)
    int countById(@Param("id") Long id);

    // Match two stops of the same trip so the interval follows the real stop order.
    @Select("""
            SELECT t.id AS trainTripId,
                   t.train_number AS trainNumber,
                   fs.name AS departureStation,
                   ts.name AS arrivalStation,
                   a.departure_at AS departureAt,
                   b.arrival_at AS arrivalAt
            FROM train_trip_stops a
            JOIN train_trip_stops b
              ON b.train_trip_id = a.train_trip_id
             AND b.stop_sequence > a.stop_sequence
            JOIN stations fs ON fs.id = a.station_id
            JOIN stations ts ON ts.id = b.station_id
            JOIN train_trips t ON t.id = a.train_trip_id
            WHERE fs.code = #{fromCode}
              AND ts.code = #{toCode}
              AND a.departure_at >= #{dayStart}
              AND a.departure_at < #{nextDayStart}
            ORDER BY a.departure_at, t.train_number, t.id
            """)
    List<TrainTripInterval> findIntervalsByCodesAndDepartureDay(
        @Param("fromCode") String fromCode,
        @Param("toCode") String toCode,
        @Param("dayStart") LocalDateTime dayStart,
        @Param("nextDayStart") LocalDateTime nextDayStart
    );
}
