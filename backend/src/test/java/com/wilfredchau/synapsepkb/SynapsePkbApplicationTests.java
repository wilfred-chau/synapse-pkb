package com.wilfredchau.synapsepkb;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilfredchau.synapsepkb.operationlog.entity.OperationLog;
import com.wilfredchau.synapsepkb.operationlog.mapper.OperationLogMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SynapsePkbApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void loginShouldReturnJwtAndCurrentUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "pkb-admin",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("pkb-admin"))
                .andExpect(jsonPath("$.data.user.spaceKey").value("personal-space"));
    }

    @Test
    void loginShouldWriteAuditLog() throws Exception {
        long beforeCount = operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getAction, "AUTH_LOGIN"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "pkb-admin",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk());

        long afterCount = operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getAction, "AUTH_LOGIN"));

        OperationLog latestLog = operationLogMapper.selectOne(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getAction, "AUTH_LOGIN")
                .orderByDesc(OperationLog::getId)
                .last("LIMIT 1"));

        org.assertj.core.api.Assertions.assertThat(afterCount).isEqualTo(beforeCount + 1);
        org.assertj.core.api.Assertions.assertThat(latestLog).isNotNull();
        org.assertj.core.api.Assertions.assertThat(latestLog.getActorUsername()).isEqualTo("pkb-admin");
        org.assertj.core.api.Assertions.assertThat(latestLog.getTargetType()).isEqualTo("USER");
        org.assertj.core.api.Assertions.assertThat(latestLog.getTargetId()).isEqualTo("1");
        org.assertj.core.api.Assertions.assertThat(latestLog.isSuccess()).isTrue();
        org.assertj.core.api.Assertions.assertThat(latestLog.getRequestId()).isNotBlank();
    }

    @Test
    void loginShouldRejectInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "pkb-admin",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("Invalid username or password"));
    }

    @Test
    void loginShouldRejectBlankUsername() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.fieldErrors.username").value("username is required"));
    }

    @Test
    void meShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void meShouldRejectInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));
    }

    @Test
    void meShouldReturnCurrentUserWhenTokenIsValid() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "pkb-admin",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = jsonNode.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.data.displayName").value("Wilfred"))
                .andExpect(jsonPath("$.data.spaceKey").value("personal-space"));
    }
}
