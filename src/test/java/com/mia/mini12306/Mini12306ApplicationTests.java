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

        assertEquals(List.of("1", "2", "3", "4"), versions);

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
                "ticket_inventory",
                "ticket_orders",
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
    }
}
