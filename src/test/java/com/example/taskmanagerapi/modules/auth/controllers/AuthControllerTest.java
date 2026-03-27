package com.example.taskmanagerapi.modules.auth.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.taskmanagerapi.infra.exception.BusinessException;
import com.example.taskmanagerapi.infra.exception.ForbiddenException;
import com.example.taskmanagerapi.infra.exception.UnauthorizedException;
import com.example.taskmanagerapi.infra.security.RateLimitFilter;
import com.example.taskmanagerapi.infra.security.SecurityFilter;
import com.example.taskmanagerapi.infra.security.TokenService;
import com.example.taskmanagerapi.modules.auth.dto.AuthResponseDTO;
import com.example.taskmanagerapi.modules.auth.dto.MessageResponseDTO;
import com.example.taskmanagerapi.modules.auth.services.AuthService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SecurityFilter securityFilter;
    @MockitoBean private RateLimitFilter rateLimitFilter;

    private AuthResponseDTO successAuthResponse;

    @BeforeEach
    void setUp() {
        successAuthResponse = new AuthResponseDTO("John Doe", "access-token", "refresh-token");
    }

    @Nested
    @DisplayName("POST /auth/login")
    class LoginEndpoint {

        @Test
        @DisplayName("should return 200 with valid credentials")
        void loginShouldReturn200() throws Exception {
            when(authService.login(any(), any(), any())).thenReturn(successAuthResponse);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"emailOrUsername": "john@test.com", "password": "password123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
        }

        @Test
        @DisplayName("should return 401 for invalid credentials")
        void loginShouldReturn401() throws Exception {
            when(authService.login(any(), any(), any()))
                    .thenThrow(new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials."));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"emailOrUsername": "john@test.com", "password": "wrong"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("should return 403 when email not verified")
        void loginShouldReturn403() throws Exception {
            when(authService.login(any(), any(), any()))
                    .thenThrow(new ForbiddenException("EMAIL_NOT_VERIFIED", "Email not verified."));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"emailOrUsername": "john@test.com", "password": "password123"}
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        }
    }

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("should return 201 on successful registration")
        void registerShouldReturn201() throws Exception {
            when(authService.register(any(), anyString()))
                    .thenReturn(new MessageResponseDTO("Registration successful!"));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "John Doe",
                                        "username": "johndoe",
                                        "email": "john@test.com",
                                        "password": "password123",
                                        "confirmPassword": "password123"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Registration successful!"));
        }

        @Test
        @DisplayName("should return 400 when passwords don't match")
        void shouldReturn400WhenPasswordsMismatch() throws Exception {
            when(authService.register(any(), anyString()))
                    .thenThrow(new BusinessException("PASSWORDS_DO_NOT_MATCH", "Passwords do not match."));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "John Doe",
                                        "username": "johndoe",
                                        "email": "john@test.com",
                                        "password": "password123",
                                        "confirmPassword": "different"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PASSWORDS_DO_NOT_MATCH"));
        }

        @Test
        @DisplayName("should return 400 for validation errors")
        void shouldReturn400ForValidationErrors() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "",
                                        "username": "",
                                        "email": "invalid-email",
                                        "password": "12",
                                        "confirmPassword": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/google")
    class GoogleLoginEndpoint {

        @Test
        @DisplayName("should return 200 with valid Google token")
        void googleLoginShouldReturn200() throws Exception {
            when(authService.googleLogin(anyString(), any(), any()))
                    .thenReturn(successAuthResponse);

            mockMvc.perform(post("/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"idToken": "valid-google-id-token"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists());
        }

        @Test
        @DisplayName("should return 401 for invalid Google token")
        void googleLoginShouldReturn401() throws Exception {
            when(authService.googleLogin(anyString(), any(), any()))
                    .thenThrow(new UnauthorizedException("INVALID_GOOGLE_TOKEN", "Invalid Google token."));

            mockMvc.perform(post("/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"idToken": "invalid-google-token"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class RefreshEndpoint {

        @Test
        @DisplayName("should return 200 with new access token")
        void refreshShouldReturn200() throws Exception {
            when(authService.refreshToken(anyString(), any()))
                    .thenReturn(new AuthResponseDTO("John Doe", "new-access-token", "refresh-token"));

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refreshToken": "refresh-token-uuid"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"));
        }

        @Test
        @DisplayName("should return 401 for invalid refresh token")
        void refreshShouldReturn401() throws Exception {
            when(authService.refreshToken(anyString(), any()))
                    .thenThrow(new UnauthorizedException("INVALID_TOKEN", "Invalid refresh token."));

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"refreshToken": "invalid-refresh-token"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /auth/verify-email")
    class VerifyEmailEndpoint {

        @Test
        @DisplayName("should return 200 on successful verification")
        void verifyEmailShouldReturn200() throws Exception {
            when(authService.verifyEmail(anyString(), any()))
                    .thenReturn(new MessageResponseDTO("Email verified successfully!"));

            mockMvc.perform(post("/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"token": "verify-token-uuid"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Email verified successfully!"));
        }
    }

    @Nested
    @DisplayName("POST /auth/forgot-password")
    class ForgotPasswordEndpoint {

        @Test
        @DisplayName("should return 200 on successful email send")
        void forgotPasswordShouldReturn200() throws Exception {
            when(authService.forgotPassword(anyString()))
                    .thenReturn(new MessageResponseDTO("Password reset email sent."));

            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "john@test.com"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset email sent."));
        }
    }

    @Nested
    @DisplayName("POST /auth/reset-password")
    class ResetPasswordEndpoint {

        @Test
        @DisplayName("should return 200 on successful password reset")
        void resetPasswordShouldReturn200() throws Exception {
            when(authService.resetPassword(any(), anyString()))
                    .thenReturn(new MessageResponseDTO("Password reset successfully."));

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "token": "reset-token",
                                        "newPassword": "newpass123",
                                        "confirmNewPassword": "newpass123"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset successfully."));
        }
    }
}
