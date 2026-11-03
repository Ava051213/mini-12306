package com.mia.mini12306.inventory.mapper;

import com.mia.mini12306.inventory.SeatCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeatSegmentMapper {

    // The first seat whose interval is free and fully described.
    @Select("""
            SELECT s.id AS seatId,
                   c.carriage_number AS carriageNumber,
                   s.seat_number AS seatNumber
            FROM train_seats s
            JOIN train_carriages c
            ON c.id = s.carriage_id
            AND c.train_trip_id = s.train_trip_id
            WHERE s.train_trip_id = #{trainTripId}
            AND s.seat_type = #{seatType}
            AND (
                SELECT COUNT(*)
                FROM train_seat_segments g
                WHERE g.train_trip_id = s.train_trip_id
                AND g.seat_id = s.id
                AND g.segment_sequence >= #{departureSequence}
                AND g.segment_sequence < #{arrivalSequence}
            ) = #{arrivalSequence} - #{departureSequence}
            AND NOT EXISTS (
                SELECT 1
                FROM train_seat_segments g
                WHERE g.train_trip_id = s.train_trip_id
                AND g.seat_id = s.id
                AND g.segment_sequence >= #{departureSequence}
                AND g.segment_sequence < #{arrivalSequence}
                AND g.seat_status <> 'AVAILABLE'
            )
            ORDER BY c.carriage_number, s.seat_number, s.id
            LIMIT 1
            """)
    SeatCandidate findFirstAvailableSeat(
            @Param("trainTripId") Long trainTripId,
            @Param("seatType") int seatType,
            @Param("departureSequence") int departureSequence,
            @Param("arrivalSequence") int arrivalSequence
    );

    // The condition keeps a half-occupied seat from being sold.
    @Update("""
            UPDATE train_seat_segments
            SET seat_status = 'OCCUPIED',
                order_id = #{orderId}
            WHERE train_trip_id = #{trainTripId}
            AND seat_id = #{seatId}
            AND segment_sequence >= #{departureSequence}
            AND segment_sequence < #{arrivalSequence}
            AND seat_status = 'AVAILABLE'
            """)
    int occupy(
            @Param("trainTripId") Long trainTripId,
            @Param("seatId") Long seatId,
            @Param("orderId") Long orderId,
            @Param("departureSequence") int departureSequence,
            @Param("arrivalSequence") int arrivalSequence
    );

    // Release only the rows this order owns.
    @Update("""
            UPDATE train_seat_segments
            SET seat_status = 'AVAILABLE',
                order_id = NULL
            WHERE order_id = #{orderId}
            AND seat_status = 'OCCUPIED'
            """)
    int releaseByOrderId(@Param("orderId") Long orderId);

    @Select("""
            SELECT COUNT(*)
            FROM train_seats
            WHERE train_trip_id = #{trainTripId}
            AND seat_type = #{seatType}
            """)
    int countSeatsByType(
            @Param("trainTripId") Long trainTripId,
            @Param("seatType") int seatType
    );

    // Seats whose segment rows do not cover the whole interval.
    @Select("""
            SELECT COUNT(*)
            FROM train_seats s
            WHERE s.train_trip_id = #{trainTripId}
            AND s.seat_type = #{seatType}
            AND (
                SELECT COUNT(*)
                FROM train_seat_segments g
                WHERE g.train_trip_id = s.train_trip_id
                AND g.seat_id = s.id
                AND g.segment_sequence >= #{departureSequence}
                AND g.segment_sequence < #{arrivalSequence}
            ) <> #{arrivalSequence} - #{departureSequence}
            """)
    int countSeatsMissingSegments(
            @Param("trainTripId") Long trainTripId,
            @Param("seatType") int seatType,
            @Param("departureSequence") int departureSequence,
            @Param("arrivalSequence") int arrivalSequence
    );
}
