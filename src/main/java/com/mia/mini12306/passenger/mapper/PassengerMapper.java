package com.mia.mini12306.passenger.mapper;

import com.mia.mini12306.passenger.Passenger;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PassengerMapper {

    @Insert("""
            INSERT INTO passengers (user_id, real_name, id_card, phone)
            VALUES (#{userId}, #{realName}, #{idCard}, #{phone})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Passenger passenger);

    @Select("""
            SELECT id, user_id AS userId, real_name AS realName,
                   id_card AS idCard, phone,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM passengers
            WHERE user_id = #{userId}
            ORDER BY id
            """)
    List<Passenger> findAllByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT id, user_id AS userId, real_name AS realName,
                   id_card AS idCard, phone,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM passengers
            WHERE id = #{id}
              AND user_id = #{userId}
            """)
    Passenger findByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    @Update("""
            UPDATE passengers
            SET real_name = #{realName},
                id_card = #{idCard},
                phone = #{phone}
            WHERE id = #{id}
              AND user_id = #{userId}
            """)
    int update(Passenger passenger);

    @Select("""
            SELECT COUNT(*)
            FROM ticket_orders
            WHERE passenger_id = #{passengerId}
            """)
    long countOrdersByPassengerId(@Param("passengerId") Long passengerId);

    @Delete("""
            DELETE FROM passengers
            WHERE id = #{id}
              AND user_id = #{userId}
            """)
    int deleteByIdAndUserId(
            @Param("id") Long id,
            @Param("userId") Long userId
    );
}
