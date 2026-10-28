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
                passenger_name, passenger_id_card, status, pay_deadline
            )
            VALUES (
                #{userId}, #{trainTripId}, #{passengerId},
                #{passengerName}, #{passengerIdCard}, #{status},
                DATE_ADD(NOW(), INTERVAL 30 MINUTE)
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
                   pay_deadline AS payDeadline,
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

    // Count only the current account's orders.
    @Select("""
            SELECT COUNT(*)
            FROM ticket_orders
            WHERE user_id = #{userId}
            """)
    long countByUserId(@Param("userId") Long userId);

    // Apply pagination in the database with a fixed order.
    @Select("""
            SELECT id,
                   user_id AS userId,
                   train_trip_id AS trainTripId,
                   passenger_id AS passengerId,
                   passenger_name AS passengerName,
                   passenger_id_card AS passengerIdCard,
                   status,
                   pay_deadline AS payDeadline,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM ticket_orders
            WHERE user_id = #{userId}
            ORDER BY id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<TicketOrder> findPageByUserId(
            @Param("userId") Long userId,
            @Param("size") int size,
            @Param("offset") long offset
    );

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

    // Only a pending order inside its payment window may be paid.
    @Update("""
            UPDATE ticket_orders
            SET status = 'PAID'
            WHERE id = #{id}
              AND user_id = #{userId}
              AND status = 'PENDING_PAYMENT'
              AND pay_deadline > NOW()
            """)
    int updateStatusIfPendingAndNotExpired(
            @Param("id") Long id,
            @Param("userId") Long userId
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
                   pay_deadline AS payDeadline,
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

    // Collect orders whose payment window has closed.
    @Select("""
            SELECT id
            FROM ticket_orders
            WHERE status = 'PENDING_PAYMENT'
              AND pay_deadline <= NOW()
            ORDER BY pay_deadline
            LIMIT #{limit}
            """)
    List<Long> findExpiredIds(@Param("limit") int limit);

    // Close the order only while it is still pending and past its deadline.
    @Update("""
            UPDATE ticket_orders
            SET status = 'EXPIRED'
            WHERE id = #{id}
              AND status = 'PENDING_PAYMENT'
              AND pay_deadline <= NOW()
            """)
    int expireIfPendingAndOverdue(@Param("id") Long id);

    // Read the trip after the status change, inside the same transaction.
    @Select("""
            SELECT train_trip_id
            FROM ticket_orders
            WHERE id = #{id}
            """)
    Long findTrainTripIdById(@Param("id") Long id);
}
