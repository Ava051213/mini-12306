package com.mia.mini12306.train.mapper;

import com.mia.mini12306.train.TrainTrip;
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
}
