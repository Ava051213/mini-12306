package com.mia.mini12306.order.mapper;

import com.mia.mini12306.order.OrderStatus;
import com.mia.mini12306.order.TicketOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TicketOrderMapper {

    // Let the database generate the ID and timestamps.
    @Insert("""
            INSERT INTO ticket_orders (
                user_id, train_trip_id, passenger_id,
                passenger_name, passenger_id_card, status
            )
            VALUES (
                #{userId}, #{trainTripId}, #{passengerId},
                #{passengerName}, #{passengerIdCard}, #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TicketOrder order);

    // Restrict order access to the owning account.
    @Select("""
            SELECT id,
                   user_id AS userId,
                   train_trip_id AS trainTripId,
                   passenger_id AS passengerId,
                   passenger_name AS passengerName,
                   passenger_id_card AS passengerIdCard,
                   status,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM ticket_orders
            WHERE id = #{id}
              AND user_id = #{userId}
            """)
    TicketOrder findByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    // Load only the current account's orders, newest first.
    @Select("""
            SELECT id,
                   user_id AS userId,
                   train_trip_id AS trainTripId,
                   passenger_id AS passengerId,
                   passenger_name AS passengerName,
                   passenger_id_card AS passengerIdCard,
                   status,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM ticket_orders
            WHERE user_id = #{userId}
            ORDER BY id DESC
            """)
    List<TicketOrder> findAllByUserId(@Param("userId") Long userId);

    // Only an owned pending order may enter a terminal state.
    @Update("""
            UPDATE ticket_orders
            SET status = #{targetStatus}
            WHERE id = #{id}
              AND user_id = #{userId}
              AND status = 'PENDING_PAYMENT'
            """)
    int updateStatusIfPending(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("targetStatus") OrderStatus targetStatus
    );

    // Read the latest state and hold the row lock until transaction completion.
    @Select("""
            SELECT id,
                   user_id AS userId,
                   train_trip_id AS trainTripId,
                   passenger_id AS passengerId,
                   passenger_name AS passengerName,
                   passenger_id_card AS passengerIdCard,
                   status,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM ticket_orders
            WHERE id = #{id}
              AND user_id = #{userId}
            FOR UPDATE
            """)
    TicketOrder findByIdAndUserIdForUpdate(
            @Param("id") Long id,
            @Param("userId") Long userId
    );
}
