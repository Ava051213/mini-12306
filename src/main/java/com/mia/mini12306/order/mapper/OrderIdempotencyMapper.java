package com.mia.mini12306.order.mapper;

import com.mia.mini12306.order.OrderIdempotency;
import com.mia.mini12306.order.PurchaseMode;
import org.apache.ibatis.annotations.*;

@Mapper
public interface OrderIdempotencyMapper {

    // Insert a placeholder or lock the existing key without replacing its data.
    @Insert("""
            INSERT INTO order_idempotency (
                user_id, idempotency_key, purchase_mode,
                train_trip_id, passenger_id, seat_type
            )
            VALUES (
                #{userId}, #{idempotencyKey}, #{purchaseMode},
                #{trainTripId}, #{passengerId}, #{seatType}
            )
            ON DUPLICATE KEY UPDATE
                idempotency_key = idempotency_key
            """)
    int reserve(
            @Param("userId") Long userId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("purchaseMode") PurchaseMode purchaseMode,
            @Param("trainTripId") Long trainTripId,
            @Param("passengerId") Long passengerId,
            @Param("seatType") Integer seatType
    );

    // Read the latest record while holding its lock until transaction completion.
    @Select("""
            SELECT user_id AS userId,
                   idempotency_key AS idempotencyKey,
                   purchase_mode AS purchaseMode,
                   train_trip_id AS trainTripId,
                   passenger_id AS passengerId,
                   seat_type AS seatType,
                   order_id AS orderId
            FROM order_idempotency
            WHERE user_id = #{userId}
              AND idempotency_key = #{idempotencyKey}
            FOR UPDATE
            """)
    OrderIdempotency findForUpdate(
            @Param("userId") Long userId,
            @Param("idempotencyKey") String idempotencyKey
    );

    // Complete the placeholder within the same transaction or order creation.
    @Update("""
            UPDATE order_idempotency
            SET order_id = #{orderId}
            WHERE user_id = #{userId}
              AND idempotency_key = #{idempotencyKey}
              AND order_id IS NULL
            """)
    int attachOrder(
            @Param("userId") Long userId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("orderId") Long orderId
    );
}
