package com.mia.mini12306.common.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerIntegrationTests.TestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestConfig {
        @Bean
        TestFailureController testFailureController() {
            return new TestFailureController();
        }
    }

    @RestController
    static class TestFailureController {
        @GetMapping("/__test/errors/unexpected")
        String fail() {
            throw new IllegalStateException("Fake-500-Sensitive-Marker");
        }
    }

    // Add the HTTP and log regression tests here.
    @Test
    void shouldHideRejectedPasswordFromResponseAndLogs(
            CapturedOutput output
    ) throws Exception {
        String marker = "Fake7!";

        mockMvc.perform(post("/api/users/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {"username":"safe_user","password":"Fake7!"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(marker)
                )));

        assertThat(output).doesNotContain(marker);
    }

    @Test
    void shouldHideRejectedPassengerDataFromResponseAndLogs(
            CapturedOutput output
    ) throws Exception {
        String idCardMarker = "Fake-ID-123";
        String phoneMarker = "Fake-PHONE-456";

        mockMvc.perform(post("/api/passengers")
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "realName":"Fake Rider",
                              "idCard":"Fake-ID-123",
                              "phone":"Fake-PHONE-456"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(idCardMarker)
                )))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(phoneMarker)
                )));

        assertThat(output)
                .doesNotContain(idCardMarker)
                .doesNotContain(phoneMarker);
    }

    @Test
    void shouldReturnSafeResponsesForPathAndJsonErrors(
            CapturedOutput output
    ) throws Exception {
        String pathMarker = "Fake-PATH-789";
        String jsonMarker = "Fake-JSON-012";

        // Reject non-positive IDs before entering the business method.
        mockMvc.perform(get("/api/orders/0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/api/trains/{id}/inventory", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("not-a-number")
                )));

        // Malformed JSON includes a marker to catch accidental input echoing.
        mockMvc.perform(post("/api/users/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"safe\",\"password\":\""
                                + jsonMarker))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(jsonMarker)
                )));

        mockMvc.perform(get("/api/orders/{id}", "not-a-number-" + pathMarker))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(pathMarker)
                )));

        // Check console logs as well as HTTP response bodies.
        assertThat(output)
                .doesNotContain(pathMarker)
                .doesNotContain(jsonMarker)
                .doesNotContain("not-a-number");
    }

    @Test
    void shouldReturnUnifiedNotFoundAndMethodNotAllowedResponses()
            throws Exception {
        mockMvc.perform(get("/__test/errors/no-such-route"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("请求地址不存在"));

        mockMvc.perform(put("/api/users/me"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("请求方法不允许"))
                .andExpect(header().string("Allow",
                        org.hamcrest.Matchers.containsString("GET")));
    }

    @Test
    void shouldHideUnexpectedExceptionDetailsFromResponseAndLogs(
            CapturedOutput output
    ) throws Exception {
        String marker = "Fake-500-Sensitive-Marker";

        mockMvc.perform(get("/__test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("服务器内部错误"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(marker)
                )));

        assertThat(output).doesNotContain(marker);
    }

    @Test
    void shouldReturnSafeMessagesForBodyAndQueryValidation(
            CapturedOutput output
    ) throws Exception {
        // Valid JSON still needs to satisfy the input constraints.
        mockMvc.perform(post("/api/users/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                 {"username":"safe_user"}
                                 """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("password：不能为空"));

        // An empty JSON object leaves both required IDs missing.
        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("passengerId：不能为空"));

        // Query parameters must be convertible to the declared Java types.
        mockMvc.perform(get("/api/trains")
                        .param("departureDate", "Fake-DATE-345")
                        .param("fromStation", "测试起点")
                        .param("toStation", "测试终点"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("departureDate：格式不正确"));

        assertThat(output).doesNotContain("Fake-DATE-345");
    }
}
