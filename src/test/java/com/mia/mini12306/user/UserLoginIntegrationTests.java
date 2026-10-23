package com.mia.mini12306.user;


import com.mia.mini12306.user.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserLoginIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldLoginQueryCurrentUserAndLogout() throws Exception {
        String username = "login_" + UUID.randomUUID()
                .toString().replace("-", "");
        String password = "Train123";

        String requestBody = objectMapper.writeValueAsString(
                new Credentials(username, password)
        );

        // 1. 准备账号，真实注册接口
        mockMvc.perform(post("/api/users/register")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated());

        String storedHash = userMapper.findByUsername(username)
                .getPasswordHash();

        // 2. 登录并取得服务端建立的会话
        MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                .contentType(APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        String loginBody = loginResult.getResponse().getContentAsString();
        assertFalse(loginBody.contains(password));
        assertFalse(loginBody.contains(storedHash));

        HttpSession loginSession = loginResult.getRequest().getSession(false);
        assertNotNull(loginSession);
        MockHttpSession session = (MockHttpSession) loginSession;

        // 3. 使用同一个会话查询当前用户
        MvcResult meResult = mockMvc.perform(get("/api/users/me")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        String meBody = meResult.getResponse().getContentAsString();
        assertFalse(meBody.contains(password));
        assertFalse(meBody.contains(storedHash));

        // 登录和当前用户接口应返回同一份资料
        assertEquals(
                objectMapper.readTree(loginBody),
                objectMapper.readTree(meBody)
        );

        // 4. 退出，使原会话失效
        mockMvc.perform(post("/api/users/logout")
                .session(session))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertTrue(session.isInvalid());

        // 5. 没有有效会话时无法查询当前用户
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"));
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        String username = "login_" + UUID.randomUUID()
                .toString().replace("-", "");

        mockMvc.perform(post("/api/users/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new Credentials(username, "Train123")
                )))
                .andExpect(status().isCreated());

        String wrongPassword = "Wrong123";
        String storedHash = userMapper.findByUsername(username)
                .getPasswordHash();

        MvcResult result = mockMvc.perform(post("/api/users/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new Credentials(username, wrongPassword)
                )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        assertNull(result.getRequest().getSession(false));

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains(wrongPassword));
        assertFalse(body.contains("Train123"));
        assertFalse(body.contains(storedHash));
    }

    @Test
    void shouldRejectUnknownUsername() throws Exception {
        String username = "missing_" + UUID.randomUUID()
                .toString().replace("-", "");
        String password = "Train123";

        MvcResult result = mockMvc.perform(post("/api/users/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new Credentials(username, password)
                )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        assertNull(result.getRequest().getSession(false));
        assertFalse(result.getResponse().getContentAsString().contains(password));
    }

    @Test
    void shouldRejectCurrentUserQueryWithoutLogin() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请先登录"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        assertNull(result.getRequest().getSession(false));
    }

    private record Credentials(String username, String password) {

    }
}
