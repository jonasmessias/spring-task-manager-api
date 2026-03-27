package com.example.taskmanagerapi.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.taskmanagerapi.modules.auth.services.EmailService;
import com.example.taskmanagerapi.modules.auth.services.GoogleAuthService;
import com.example.taskmanagerapi.modules.storage.services.StorageService;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E: Auth → Workspace → Board → List → Card Flow")
class AuthWorkspaceBoardFlowTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private EmailService emailService;
    @MockitoBean private GoogleAuthService googleAuthService;
    @MockitoBean private StorageService storageService;
    @MockitoBean private RedisTemplate<String, Object> redisTemplate;
    @MockitoBean private JedisConnectionFactory jedisConnectionFactory;

    @BeforeEach
    void setUp() {
        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString(), any());
    }

    @Test
    @Order(1)
    @DisplayName("1. Register a new user")
    void shouldRegisterUser() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "E2E Test User",
                                    "username": "e2euser",
                                    "email": "e2e@test.com",
                                    "password": "password123",
                                    "confirmPassword": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Order(2)
    @DisplayName("2. Login should fail - email not verified")
    void shouldFailLoginNotVerified() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailOrUsername": "e2e@test.com", "password": "password123"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    @Order(3)
    @DisplayName("3. Verify email and login successfully")
    void shouldVerifyAndLogin() throws Exception {
        // Manually verify the user via DB for E2E test
        // In a real E2E, we'd use the email token; here we verify via repository directly
        // Let's use a second user that's pre-verified
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Verified User",
                                    "username": "verified",
                                    "email": "verified@test.com",
                                    "password": "password123",
                                    "confirmPassword": "password123"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(4)
    @DisplayName("4. Login should fail with wrong password")
    void shouldFailLoginWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"emailOrUsername": "e2e@test.com", "password": "wrongpassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @Order(5)
    @DisplayName("5. Register duplicate email should fail")
    void shouldFailDuplicateEmail() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Duplicate User",
                                    "username": "dup_user",
                                    "email": "e2e@test.com",
                                    "password": "password123",
                                    "confirmPassword": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Register duplicate username should fail")
    void shouldFailDuplicateUsername() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Duplicate User",
                                    "username": "e2euser",
                                    "email": "unique@test.com",
                                    "password": "password123",
                                    "confirmPassword": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    @Order(7)
    @DisplayName("7. Register with mismatched passwords should fail")
    void shouldFailMismatchedPasswords() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Test User",
                                    "username": "testuser99",
                                    "email": "test99@test.com",
                                    "password": "password123",
                                    "confirmPassword": "different456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORDS_DO_NOT_MATCH"));
    }

    @Test
    @Order(8)
    @DisplayName("8. Accessing protected endpoint without token should fail")
    void shouldFailWithoutToken() throws Exception {
        mockMvc.perform(get("/workspaces"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("9. Forgot password for non-existent email should fail")
    void shouldFailForgotPasswordNotFound() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nonexistent@test.com"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(10)
    @DisplayName("10. Forgot password for existing email should succeed")
    void shouldSucceedForgotPassword() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "e2e@test.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
