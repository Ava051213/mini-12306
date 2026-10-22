package com.mia.mini12306.user;

import org.assertj.core.api.Java6BDDSoftAssertionsProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JacksonComponentModule;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import javax.swing.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserRegistrationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Autowired
    private JacksonComponentModule jsonComponentModule;

    @Test
    void shouldRegisterUserAndStorePasswordHash() throws Exception {
        String username = "test_" + UUID.randomUUID().toString().replace("-", "");
        String password = "Train123";

        MvcResult result = mockMvc.perform(post("/api/users/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterRequest(username, password)
                )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE username = ?",
                String.class,
                username
        );

        assertNotEquals(password, storedHash);
        assertTrue(passwordEncoder.matches(password, storedHash));
        assertFalse(result.getResponse().getContentAsString().contains(storedHash));
    }

    @Test
    void shouldRejectDuplicateUsername() throws Exception {
        String username = "test_" + UUID.randomUUID().toString().replace("-", "");
        String requestBody = objectMapper.writeValueAsString(
                new RegisterRequest(username, "Train123")
        );

        mockMvc.perform(post("/api/users/register")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/register")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldRejectInvalidRegistrationInput() throws Exception {
        mockMvc.perform(post("/api/users/register")
                .contentType(APPLICATION_JSON)
                .content("""
                        {"username":"x","password":"short"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectPasswordOverBcryptByteLimit() throws Exception {
        String password = "中".repeat(25);

        mockMvc.perform(post("/api/users/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterRequest("valid_user", password)
                )))
                .andExpect(status().isBadRequest());
    }

    private record RegisterRequest(String username, String password) {

    }
}
