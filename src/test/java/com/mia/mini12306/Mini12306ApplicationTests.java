package com.mia.mini12306;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class Mini12306ApplicationTests {

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
        assertEquals("mini_12306", database);
    }
}