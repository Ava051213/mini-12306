package com.mia.mini12306.station.mapper;

import com.mia.mini12306.station.RouteBoundaryStop;
import com.mia.mini12306.station.Station;
import com.mia.mini12306.station.TrainTripStopRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StationQueryMapper {

    @Select("""
            SELECT id,
                   code,
                   name,
                   spell,
                   region_name AS regionName
            FROM stations
            ORDER BY code
            """)
    List<Station> findAllOrderByCode();

    // Numeric stop order keeps 2 before 10, which string order would break.
    @Select("""
            SELECT s.stop_sequence AS stopSequence,
                   st.name AS stationName,
                   s.arrival_at AS arrivalTime,
                   s.departure_at AS departureTime
            FROM train_trip_stops s
            JOIN stations st ON st.id = s.station_id
            WHERE s.train_trip_id = #{trainTripId}
            ORDER BY s.stop_sequence
            """)
    List<TrainTripStopRow> findStopsByTrainTripId(
            @Param("trainTripId") Long trainTripId
    );

    // Both codes must exist before the interval query can judge the route.
    @Select("""
            SELECT COUNT(*)
            FROM stations
            WHERE code IN (#{fromCode}, #{toCode})
            """)
    int countByCodes(
            @Param("fromCode") String fromCode,
            @Param("toCode") String toCode
    );

    // Only the two ends of the route; a single-stop trip returns one row.
    @Select("""
            SELECT s.stop_sequence AS stopSequence,
                   st.code AS stationCode,
                   st.name AS stationName,
                   s.arrival_at AS arrivalAt,
                   s.departure_at AS departureAt
            FROM train_trip_stops s
            JOIN stations st ON st.id = s.station_id
            WHERE s.train_trip_id = #{trainTripId}
              AND s.stop_sequence IN (
                  (SELECT MIN(stop_sequence)
                   FROM train_trip_stops
                   WHERE train_trip_id = #{trainTripId}),
                  (SELECT MAX(stop_sequence)
                   FROM train_trip_stops
                   WHERE train_trip_id = #{trainTripId})
              )
            ORDER BY s.stop_sequence
            """)
    List<RouteBoundaryStop> findRouteBoundaryStops(
            @Param("trainTripId") Long trainTripId
    );
}
