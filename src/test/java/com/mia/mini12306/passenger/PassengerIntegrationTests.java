package com.mia.mini12306.passenger;

import com.mia.mini12306.passenger.dto.PassengerResponse;
import com.mia.mini12306.passenger.dto.PassengerSaveRequest;
import com.mia.mini12306.user.CurrentUserSession;
import com.zaxxer.hikari.util.Credentials;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationListener;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PassengerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateQueryAndUpdatePassenger() throws Exception {
        MockHttpSession session = registerAndLogin();

        mockMvc.perform(get("/api/passengers").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // Use synthetic documents and two distinct passenger IDs.
        List<PassengerSaveRequest> requests = List.of(
                new PassengerSaveRequest(
                        "Test Rider", "00000000000000001x", "13800000000"
                ),
                new PassengerSaveRequest(
                        "Backup Rider", "00000000000000002x", "13800000001"
                )
        );

        for(PassengerSaveRequest request : requests) {
            mockMvc.perform(post("/api/passengers")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().string(""));
        }

        MvcResult listResult = mockMvc.perform(get("/api/passengers")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].idCard").value("00000000000000001X"))
                .andExpect(jsonPath("$[1].idCard").value("00000000000000002X"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].updatedAt").exists())
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andReturn();

        var passengers = objectMapper.readTree(
                listResult.getResponse().getContentAsByteArray()
        );
        Long userId = (Long) session.getAttribute(
                CurrentUserSession.LOGIN_USER_ID
        );
        assertNotNull(userId);

        // Prevent swapped arguments from passing by coincidence.
        var selected = passengers.get(0);
        if(selected.get("id").asLong() == userId.longValue()) {
            selected = passengers.get(1);
        }

        Long passengerId = selected.get("id").asLong();
        String idCard = selected.get("idCard").asString();
        assertNotEquals(userId.longValue(), passengerId);

        mockMvc.perform(get("/api/passengers/{id}", passengerId)
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passengerId))
                .andExpect(jsonPath("$.realName").value(selected.get("realName").asString()))
                .andExpect(jsonPath("$.idCard").value(idCard));

        PassengerSaveRequest updateRequest = new PassengerSaveRequest(
                "Updated Rider", idCard, "13800000002"
        );

        mockMvc.perform(put("/api/passengers/{id}", passengerId)
                .session(session)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(passengerId))
                .andExpect(jsonPath("$.realName").value("Updated Rider"))
                .andExpect(jsonPath("$.idCard").value(idCard))
                .andExpect(jsonPath("$.phone").value("13800000002"));

        // Verify the update was persisted.
        mockMvc.perform(get("/api/passengers/{id}", passengerId)
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realName").value("Updated Rider"))
                .andExpect(jsonPath("$.idCard").value(idCard))
                .andExpect(jsonPath("$.phone").value("13800000002"));
    }

    @Test
    void shouldRejectPassengerRequestsWithoutLogin() throws Exception {
        String body = objectMapper.writeValueAsString(
                new PassengerSaveRequest(
                        "Test Rider", "00000000000000001X", "13800000000"
                )
        );

        // Use valid input to isolate authentication checks.
        var requests = List.of(
                get("/api/passengers"),
                get("/api/passengers/{id}", 1L),
                post("/api/passengers")
                        .contentType(APPLICATION_JSON)
                        .content(body),
                put("/api/passengers/{id}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(body)
        );

        for(var request : requests) {
            // Deliberately omit the session
            MvcResult result = mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("请先登录"))
                    .andReturn();

            assertNull(result.getRequest().getSession(false));
        }
    }

    @Test
    void shouldIsolatePassengersBetweenAccounts() throws Exception {
        MockHttpSession ownerSession = registerAndLogin();
        MockHttpSession otherSession = registerAndLogin();

        PassengerResponse first = createPassenger(
                ownerSession,
                new PassengerSaveRequest(
                        "Owner Rider", "00000000000000003X", "13800000003"
                )
        );
        PassengerResponse second = createPassenger(
                ownerSession,
                new PassengerSaveRequest(
                        "Backup Rider", "00000000000000004X", "13800000004"
                )
        );

        Long otherUserId = (Long) otherSession.getAttribute(
                CurrentUserSession.LOGIN_USER_ID
        );
        assertNotNull(otherUserId);

        // Keep the target record ID different from the requesting user ID.
        PassengerResponse target = first.id().equals(otherUserId) ? second : first;
        assertNotEquals(otherUserId, target.id());

        // The same document may belong to separate records under two accounts.
        PassengerResponse otherPassenger = createPassenger(
                otherSession,
                new PassengerSaveRequest(
                        "Other Rider", target.idCard(), "13800000005"
                )
        );

        mockMvc.perform(get("/api/passengers").session(otherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(otherPassenger.id()))
                .andExpect(jsonPath("$[0].realName").value("Other Rider"));

        mockMvc.perform(get("/api/passengers/{id}", target.id())
                .session(otherSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("乘车人不存在"));

        PassengerSaveRequest forbiddenUpdate = new PassengerSaveRequest(
                "Changed By Other", target.idCard(), "13800000006"
        );

        mockMvc.perform(put("/api/passengers/{id}", target.id())
                .session(otherSession)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forbiddenUpdate)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("乘车人不存在"));

        // A rejected update must leave the owner's record unchanged.
        MvcResult result = mockMvc.perform(
                get("/api/passengers/{id}", target.id())
                        .session(ownerSession))
                .andExpect(status().isOk())
                .andReturn();

        PassengerResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                PassengerResponse.class
        );
        assertEquals(target, actual);
    }

    @Test
    void shouldRejectInvalidPassengerInput() throws Exception {
        MockHttpSession session = registerAndLogin();

        PassengerResponse original = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Original Rider", "00000000000000006X", "13800000007"
                )
        );

        String name = "Valid Rider";
        String idCard = "00000000000000007X";
        String phone = "13800000008";

        // Change one field at a time to isolate each validation rule.
        List<PassengerSaveRequest> invalidReqeusts = List.of(
                new PassengerSaveRequest(null, idCard, phone),
                new PassengerSaveRequest("   ", idCard, phone),
                new PassengerSaveRequest("A", idCard, phone),
                new PassengerSaveRequest("N".repeat(51), idCard, phone),

                new PassengerSaveRequest(name, null, phone),
                new PassengerSaveRequest(name, "", phone),
                new PassengerSaveRequest(name, "00000000000000007Z", phone),

                new PassengerSaveRequest(name, idCard, null),
                new PassengerSaveRequest(name, idCard, ""),
                new PassengerSaveRequest(name, idCard, "12800000000")
        );

        for(PassengerSaveRequest request : invalidReqeusts) {
            String body = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/passengers")
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(put("/api/passengers/{id}", original.id())
                    .session(session)
                    .contentType(APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isBadRequest());
        }

        // Invalid requests must neither insert nor modify records.
        mockMvc.perform(get("/api/passengers").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        MvcResult result = mockMvc.perform(
                get("/api/passengers/{id}", original.id())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        PassengerResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                PassengerResponse.class
        );
        assertEquals(original, actual);
    }

    @Test
    void shouldRejectDuplicatePassengerDocuments() throws Exception {
        MockHttpSession session = registerAndLogin();

        PassengerResponse first = createPassenger(
                session,
                new PassengerSaveRequest(
                        "First Rider", "00000000000000008x", "13800000009"
                )
        );
        PassengerResponse second = createPassenger(
                session,
                new PassengerSaveRequest(
                        "Second Rider", "00000000000000009X", "13800000010"
                )
        );

        // Case normalization must not bypass the unique constraint.
        PassengerSaveRequest duplicate = new PassengerSaveRequest(
                "Duplicate Rider", "00000000000000008X", "13800000011"
        );

        mockMvc.perform(post("/api/passengers")
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("该乘车人已添加"));

        PassengerSaveRequest conflictingUpdate = new PassengerSaveRequest(
                first.realName(), second.idCard(), first.phone()
        );

        mockMvc.perform(put("/api/passengers/{id}", first.id())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictingUpdate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("该乘车人已添加"));

        MvcResult result = mockMvc.perform(get("/api/passengers")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        PassengerResponse[] actual = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                PassengerResponse[].class
        );

        // Both failed operations must preserve the original records.
        assertArrayEquals(new PassengerResponse[]{first, second}, actual);
    }

    private MockHttpSession registerAndLogin() throws Exception {
        String username = "passenger_" + UUID.randomUUID()
                .toString().replace("-", "");
        String credentials = objectMapper.writeValueAsString(
                new Credentials(username, "Train123"));

        // Obtain a session through the real authentication flow.
        mockMvc.perform(post("/api/users/register")
                .contentType(APPLICATION_JSON)
                .content(credentials))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/users/login")
                .contentType(APPLICATION_JSON)
                .content(credentials))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    private PassengerResponse createPassenger(
            MockHttpSession session,
            PassengerSaveRequest request
    ) throws Exception {
        mockMvc.perform(post("/api/passengers")
                .session(session)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/api/passengers")
                .session(session))
                .andExpect(status().isOk())
                .andReturn();

        PassengerResponse[] passengers = objectMapper.readValue(
                result.getResponse().getContentAsByteArray(),
                PassengerResponse[].class
        );

        // Lists are ordered by generated ID; the newest record is last.
        assertTrue(passengers.length > 0);
        return passengers[passengers.length - 1];
    }

    private record Credentials(String username, String password) {

    }
}
