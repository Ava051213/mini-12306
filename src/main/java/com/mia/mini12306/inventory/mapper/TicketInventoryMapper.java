package com.mia.mini12306.inventory.mapper;

import com.mia.mini12306.inventory.TicketInventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TicketInventoryMapper {

    // Map database columns to Java properties explicitly.
    @Select("""
            SELECT train_trip_id AS trainTripId,
                   total_tickets AS totalTickets,
                   remaining_tickets AS remainingTickets
            FROM ticket_inventory
            WHERE train_trip_id = #{trainTripId}
            """)
    TicketInventory findByTrainTripId(
            @Param("trainTripId") Long trainTripId
    );

    // Check availability of decrease stock in one database statement.
    @Update("""
            UPDATE ticket_inventory
            SET remaining_tickets = remaining_tickets - 1
            WHERE train_trip_id = #{trainTripId}
              AND remaining_tickets > 0
            """)
    int decrementIfAvailable(
            @Param("trainTripId") Long trainTripId
    );

    // Restore one ticket without exceeding the configured capacity.
    @Update("""
            UPDATE ticket_inventory
            SET remaining_tickets = remaining_tickets + 1
            WHERE train_trip_id = #{trainTripId}
              AND remaining_tickets < total_tickets
            """)
    int restoreOneIfNotFull(
            @Param("trainTripId") Long trainTripId
    );
}
