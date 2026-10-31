package com.mia.mini12306;

import com.mia.mini12306.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Mini12306ApplicationTests extends DatabaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldConnectToProjectDatabases() {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1", Integer.class
        );
        assertEquals(Integer.valueOf(1), result);

        String database = jdbcTemplate.queryForObject(
                "SELECT DATABASE()", String.class
        );
        assertEquals("mini_12306_test", database);
    }

    @Test
    void shouldUseBeijingTimeForDatabaseSession() {
        // Verify the application's actual JDBC session.
        Long offsetMinutes = jdbcTemplate.queryForObject(
                """
                SELECT TIMESTAMPDIFF(
                    MINUTE, UTC_TIMESTAMP(), NOW()
                    )
                """,
                Long.class
                );

        assertEquals(Long.valueOf(480), offsetMinutes);
    }

    @Test
    void shouldRecordSuccessfulMigrations() {
        // Verify migration history in the actual test database.
        List<String> versions = jdbcTemplate.queryForList("""
            SELECT version
            FROM flyway_schema_history
            WHERE success = 1
            ORDER BY installed_rank
            """, String.class);

        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7"), versions);

        Long failedCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM flyway_schema_history
            WHERE success = 0
            """, Long.class);

        assertEquals(Long.valueOf(0), failedCount);
    }

    @Test
    void shouldHaveExpectedTablesAndOrderIndex() {
        // Inspect only the database used by this connection.
        List<String> tables = jdbcTemplate.queryForList("""
            SELECT TABLE_NAME
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_TYPE = 'BASE TABLE'
            ORDER BY TABLE_NAME
            """, String.class);

        assertEquals(List.of(
                "flyway_schema_history",
                "order_idempotency",
                "passengers",
                "stations",
                "ticket_inventory",
                "ticket_orders",
                "train_trip_interval_prices",
                "train_trip_stops",
                "train_trips",
                "users"
        ), tables);

        // Column order matters for the account order-list index.
        List<String> indexColumns = jdbcTemplate.queryForList("""
            SELECT COLUMN_NAME
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'ticket_orders'
              AND INDEX_NAME = 'idx_ticket_orders_user_id'
            ORDER BY SEQ_IN_INDEX
            """, String.class);

        assertEquals(List.of("user_id", "id"), indexColumns);

        // The timeout scan relies on this index.
        List<String> deadlineIndexColumns = jdbcTemplate.queryForList("""
            SELECT COLUMN_NAME
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'ticket_orders'
              AND INDEX_NAME = 'idx_ticket_orders_status_deadline'
            ORDER BY SEQ_IN_INDEX
            """, String.class);

        assertEquals(List.of("status", "pay_deadline"), deadlineIndexColumns);

        // Every order must carry a deadline.
        String nullable = jdbcTemplate.queryForObject("""
                SELECT IS_NULLABLE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'ticket_orders'
                  AND COLUMN_NAME = 'pay_deadline'
                """, String.class);

        assertEquals("NO", nullable);

        // Station codes are the stable key of the dictionary.
        List<String> stationCodeIndex = jdbcTemplate.queryForList("""
            SELECT COLUMN_NAME
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'stations'
              AND INDEX_NAME = 'uk_stations_code'
            ORDER BY SEQ_IN_INDEX
            """, String.class);

        assertEquals(List.of("code"), stationCodeIndex);

        // A trip cannot repeat a stop position.
        List<String> stopSequenceIndex = jdbcTemplate.queryForList("""
            SELECT COLUMN_NAME
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'train_trip_stops'
              AND INDEX_NAME = 'uk_train_trip_stops_trip_sequence'
            ORDER BY SEQ_IN_INDEX
            """, String.class);

        assertEquals(List.of("train_trip_id", "stop_sequence"),
                stopSequenceIndex);

        // Stops must reference one trip and one station.
        List<String> stopForeignTables = jdbcTemplate.queryForList("""
            SELECT REFERENCED_TABLE_NAME
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'train_trip_stops'
              AND REFERENCED_TABLE_NAME IS NOT NULL
            ORDER BY CONSTRAINT_NAME
            """, String.class);

        assertEquals(List.of("stations", "train_trips"), stopForeignTables);

        // One price per trip, interval and seat type.
        List<String> priceIndexColumns = jdbcTemplate.queryForList("""
            SELECT COLUMN_NAME
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'train_trip_interval_prices'
              AND INDEX_NAME = 'uk_train_trip_interval_prices'
            ORDER BY SEQ_IN_INDEX
            """, String.class);

        assertEquals(List.of("train_trip_id", "departure_sequence",
                "arrival_sequence", "seat_type"), priceIndexColumns);

        // Each composite foreign key contributes one row per column.
        List<String> priceForeignColumns = jdbcTemplate.queryForList("""
            SELECT CONCAT(COLUMN_NAME, '->', REFERENCED_COLUMN_NAME)
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'train_trip_interval_prices'
              AND REFERENCED_TABLE_NAME = 'train_trip_stops'
            ORDER BY CONSTRAINT_NAME, ORDINAL_POSITION
            """, String.class);

        assertEquals(List.of(
                "train_trip_id->train_trip_id",
                "arrival_sequence->stop_sequence",
                "train_trip_id->train_trip_id",
                "departure_sequence->stop_sequence"
        ), priceForeignColumns);

        List<String> priceChecks = jdbcTemplate.queryForList("""
            SELECT CONSTRAINT_NAME
            FROM information_schema.CHECK_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = DATABASE()
              AND CONSTRAINT_NAME LIKE 'chk_train_trip_interval_prices%'
            ORDER BY CONSTRAINT_NAME
            """, String.class);

        assertEquals(List.of(
                "chk_train_trip_interval_prices_order",
                "chk_train_trip_interval_prices_price",
                "chk_train_trip_interval_prices_seat_type"
        ), priceChecks);
    }

    @Test
    void shouldConstrainOrderPriceSnapshot() {
        // Priced orders must carry the full snapshot; legacy rows must not.
        List<String> orderChecks = jdbcTemplate.queryForList("""
        SELECT CONSTRAINT_NAME
        FROM information_schema.CHECK_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND CONSTRAINT_NAME LIKE 'chk_ticket_orders_%'
        ORDER BY CONSTRAINT_NAME
        """, String.class);

        assertEquals(List.of(
                "chk_ticket_orders_amount",
                "chk_ticket_orders_legacy_snapshot",
                "chk_ticket_orders_priced_snapshot",
                "chk_ticket_orders_purchase_mode",
                "chk_ticket_orders_seat_type",
                "chk_ticket_orders_sequence_order"
        ), orderChecks);

        List<String> idempotencyChecks = jdbcTemplate.queryForList("""
        SELECT CONSTRAINT_NAME
        FROM information_schema.CHECK_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND CONSTRAINT_NAME LIKE 'chk_order_idempotency_%'
        ORDER BY CONSTRAINT_NAME
        """, String.class);

        assertEquals(List.of(
                "chk_order_idempotency_legacy_seat",
                "chk_order_idempotency_priced_seat",
                "chk_order_idempotency_purchase_mode",
                "chk_order_idempotency_seat_type"
        ), idempotencyChecks);

        // Only the discriminator is mandatory on each table.
        List<String> orderRequired = jdbcTemplate.queryForList("""
        SELECT COLUMN_NAME
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ticket_orders'
          AND COLUMN_NAME IN (
              'purchase_mode', 'departure_sequence', 'arrival_sequence',
              'departure_station_code', 'departure_station_name',
              'departure_at', 'arrival_station_code',
              'arrival_station_name', 'arrival_at',
              'seat_type', 'amount_cents'
          )
          AND IS_NULLABLE = 'NO'
        ORDER BY COLUMN_NAME
        """, String.class);

        assertEquals(List.of("purchase_mode"), orderRequired);

        List<String> idempotencyRequired = jdbcTemplate.queryForList("""
        SELECT COLUMN_NAME
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'order_idempotency'
          AND COLUMN_NAME IN ('purchase_mode', 'seat_type')
          AND IS_NULLABLE = 'NO'
        ORDER BY COLUMN_NAME
        """, String.class);

        assertEquals(List.of("purchase_mode"), idempotencyRequired);
    }
}
