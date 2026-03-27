package com.example.taskmanagerapi.modules.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.taskmanagerapi.infra.exception.BusinessException;
import com.example.taskmanagerapi.infra.exception.ForbiddenException;
import com.example.taskmanagerapi.infra.exception.ResourceNotFoundException;
import com.example.taskmanagerapi.infra.exception.UnauthorizedException;
import com.example.taskmanagerapi.infra.security.TokenService;
import com.example.taskmanagerapi.modules.auth.domain.EmailVerificationToken;
import com.example.taskmanagerapi.modules.auth.domain.PasswordResetToken;
import com.example.taskmanagerapi.modules.auth.domain.RefreshToken;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.dto.AuthResponseDTO;
import com.example.taskmanagerapi.modules.auth.dto.LoginRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.MessageResponseDTO;
import com.example.taskmanagerapi.modules.auth.dto.RegisterRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.ResetPasswordDTO;
import com.example.taskmanagerapi.modules.auth.repositories.EmailVerificationRepository;
import com.example.taskmanagerapi.modules.auth.repositories.PasswordResetRepository;
import com.example.taskmanagerapi.modules.auth.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuditLogService auditLogService;
    @Mock private EmailService emailService;
    @Mock private EmailVerificationRepository emailVerificationRepository;
    @Mock private PasswordResetRepository passwordResetRepository;
    @Mock private GoogleAuthService googleAuthService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:4200");

        testUser = new User();
        testUser.setId("user-1");
        testUser.setName("John Doe");
        testUser.setEmail("john@test.com");
        testUser.setUsername("johndoe");
        testUser.setPassword("encoded-password");
        testUser.setEmailVerified(true);
        testUser.setProvider("local");

        testRefreshToken = new RefreshToken();
        testRefreshToken.setToken("refresh-token-uuid");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpirationDate(LocalDateTime.now().plusDays(7));
        testRefreshToken.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should login successfully with email")
        void shouldLoginWithEmail() {
            LoginRequestDTO dto = new LoginRequestDTO("john@test.com", "password123");

            when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
            when(tokenService.generateToken(testUser)).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(eq(testUser), anyString(), anyString()))
                    .thenReturn(testRefreshToken);

            AuthResponseDTO result = authService.login(dto, "127.0.0.1", "Mozilla");

            assertThat(result.name()).isEqualTo("John Doe");
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token-uuid");
            verify(auditLogService).logLogin(testUser, "127.0.0.1", "Mozilla");
        }

        @Test
        @DisplayName("should login successfully with username")
        void shouldLoginWithUsername() {
            LoginRequestDTO dto = new LoginRequestDTO("johndoe", "password123");

            when(userRepository.findByEmail("johndoe")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
            when(tokenService.generateToken(testUser)).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(eq(testUser), anyString(), anyString()))
                    .thenReturn(testRefreshToken);

            AuthResponseDTO result = authService.login(dto, "127.0.0.1", "Mozilla");

            assertThat(result.name()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should throw UnauthorizedException for invalid credentials")
        void shouldThrowForInvalidCredentials() {
            LoginRequestDTO dto = new LoginRequestDTO("john@test.com", "wrong-password");

            when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(dto, "127.0.0.1", "Mozilla"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("should throw UnauthorizedException when user not found")
        void shouldThrowWhenUserNotFound() {
            LoginRequestDTO dto = new LoginRequestDTO("unknown@test.com", "password123");

            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("unknown@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(dto, "127.0.0.1", "Mozilla"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("should throw UnauthorizedException for Google-only account")
        void shouldThrowForGoogleOnlyAccount() {
            testUser.setPassword(null);
            LoginRequestDTO dto = new LoginRequestDTO("john@test.com", "password123");

            when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(dto, "127.0.0.1", "Mozilla"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when email not verified")
        void shouldThrowWhenEmailNotVerified() {
            testUser.setEmailVerified(false);
            LoginRequestDTO dto = new LoginRequestDTO("john@test.com", "password123");

            when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(dto, "127.0.0.1", "Mozilla"))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("googleLogin")
    class GoogleLogin {

        @Test
        @DisplayName("should login successfully with Google token")
        void shouldLoginWithGoogle() {
            when(googleAuthService.verifyAndGetUser("google-id-token")).thenReturn(testUser);
            when(tokenService.generateToken(testUser)).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(eq(testUser), anyString(), anyString()))
                    .thenReturn(testRefreshToken);

            AuthResponseDTO result = authService.googleLogin("google-id-token", "127.0.0.1", "Mozilla");

            assertThat(result.name()).isEqualTo("John Doe");
            assertThat(result.accessToken()).isEqualTo("access-token");
            verify(auditLogService).logLogin(testUser, "127.0.0.1", "Mozilla");
        }

        @Test
        @DisplayName("should throw UnauthorizedException for invalid Google token")
        void shouldThrowForInvalidGoogleToken() {
            when(googleAuthService.verifyAndGetUser("invalid-token"))
                    .thenThrow(new IllegalArgumentException("Invalid token"));

            assertThatThrownBy(() -> authService.googleLogin("invalid-token", "127.0.0.1", "Mozilla"))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register successfully")
        void shouldRegister() {
            RegisterRequestDTO dto = new RegisterRequestDTO(
                    "John Doe", "johndoe", "john@test.com", "password123", "password123");

            when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            MessageResponseDTO result = authService.register(dto, "127.0.0.1");

            assertThat(result.message()).contains("Registration successful");
            verify(userRepository).save(any(User.class));
            verify(auditLogService).logRegistration(any(User.class), eq("127.0.0.1"));
        }

        @Test
        @DisplayName("should throw BusinessException when passwords don't match")
        void shouldThrowWhenPasswordsMismatch() {
            RegisterRequestDTO dto = new RegisterRequestDTO(
                    "John Doe", "johndoe", "john@test.com", "password123", "different");

            assertThatThrownBy(() -> authService.register(dto, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("should throw BusinessException when email exists")
        void shouldThrowWhenEmailExists() {
            RegisterRequestDTO dto = new RegisterRequestDTO(
                    "John Doe", "johndoe", "john@test.com", "password123", "password123");

            when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.register(dto, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("should throw BusinessException when username exists")
        void shouldThrowWhenUsernameExists() {
            RegisterRequestDTO dto = new RegisterRequestDTO(
                    "John Doe", "johndoe", "john@test.com", "password123", "password123");

            when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.register(dto, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTest {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshToken() {
            when(refreshTokenService.validateRefreshToken("refresh-token-uuid"))
                    .thenReturn(Optional.of(testRefreshToken));
            when(tokenService.generateToken(testUser)).thenReturn("new-access-token");

            AuthResponseDTO result = authService.refreshToken("refresh-token-uuid", "127.0.0.1");

            assertThat(result.accessToken()).isEqualTo("new-access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token-uuid");
            verify(auditLogService).logTokenRefresh(testUser, "127.0.0.1");
        }

        @Test
        @DisplayName("should throw UnauthorizedException for invalid refresh token")
        void shouldThrowForInvalidRefreshToken() {
            when(refreshTokenService.validateRefreshToken("invalid-token"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken("invalid-token", "127.0.0.1"))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("should logout successfully")
        void shouldLogout() {
            doNothing().when(refreshTokenService).deleteRefreshToken("refresh-token-uuid");
            doNothing().when(auditLogService).logLogout(testUser, "127.0.0.1");

            authService.logout(testUser, "refresh-token-uuid", "127.0.0.1");

            verify(refreshTokenService).deleteRefreshToken("refresh-token-uuid");
            verify(auditLogService).logLogout(testUser, "127.0.0.1");
        }

        @Test
        @DisplayName("should logout from all devices")
        void shouldLogoutAll() {
            doNothing().when(refreshTokenService).deleteAllUserTokens(testUser);
            doNothing().when(auditLogService).logLogoutAll(testUser, "127.0.0.1");

            authService.logoutAll(testUser, "127.0.0.1");

            verify(refreshTokenService).deleteAllUserTokens(testUser);
            verify(auditLogService).logLogoutAll(testUser, "127.0.0.1");
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("should verify email successfully")
        void shouldVerifyEmail() {
            EmailVerificationToken token = new EmailVerificationToken();
            token.setToken("verify-token");
            token.setEmail("john@test.com");
            token.setExpirationDate(LocalDateTime.now().plusHours(24));

            when(emailVerificationRepository.findByToken("verify-token"))
                    .thenReturn(Optional.of(token));
            when(userRepository.findByEmail("john@test.com"))
                    .thenReturn(Optional.of(testUser));
            testUser.setEmailVerified(false);

            MessageResponseDTO result = authService.verifyEmail("verify-token", "127.0.0.1");

            assertThat(result.message()).contains("verified successfully");
            verify(userRepository).save(testUser);
            assertThat(testUser.isEmailVerified()).isTrue();
        }

        @Test
        @DisplayName("should throw BusinessException for invalid token")
        void shouldThrowForInvalidToken() {
            when(emailVerificationRepository.findByToken("invalid"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail("invalid", "127.0.0.1"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("should throw BusinessException for expired token")
        void shouldThrowForExpiredToken() {
            EmailVerificationToken token = new EmailVerificationToken();
            token.setToken("expired-token");
            token.setEmail("john@test.com");
            token.setExpirationDate(LocalDateTime.now().minusHours(1));

            when(emailVerificationRepository.findByToken("expired-token"))
                    .thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.verifyEmail("expired-token", "127.0.0.1"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        @Test
        @DisplayName("should send password reset email")
        void shouldSendResetEmail() {
            when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(testUser));
            when(passwordResetRepository.save(any(PasswordResetToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            MessageResponseDTO result = authService.forgotPassword("john@test.com");

            assertThat(result.message()).contains("Password reset email sent");
            verify(emailService).sendHtmlEmail(eq("john@test.com"), anyString(), eq("password-reset"), any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when email not found")
        void shouldThrowWhenEmailNotFound() {
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.forgotPassword("unknown@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("should reset password successfully")
        void shouldResetPassword() {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken("reset-token");
            resetToken.setEmail("john@test.com");
            resetToken.setExpirationDate(LocalDateTime.now().plusMinutes(30));

            ResetPasswordDTO dto = new ResetPasswordDTO("reset-token", "newpass123", "newpass123");

            when(passwordResetRepository.findByToken("reset-token"))
                    .thenReturn(Optional.of(resetToken));
            when(userRepository.findByEmail("john@test.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newpass123")).thenReturn("new-encoded-password");

            MessageResponseDTO result = authService.resetPassword(dto, "127.0.0.1");

            assertThat(result.message()).contains("Password reset successfully");
            verify(userRepository).save(testUser);
            verify(refreshTokenService).deleteAllUserTokens(testUser);
        }

        @Test
        @DisplayName("should throw BusinessException when passwords don't match")
        void shouldThrowWhenPasswordsMismatch() {
            ResetPasswordDTO dto = new ResetPasswordDTO("reset-token", "newpass123", "different");

            assertThatThrownBy(() -> authService.resetPassword(dto, "127.0.0.1"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("should throw UnauthorizedException for expired token")
        void shouldThrowForExpiredResetToken() {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken("expired-token");
            resetToken.setEmail("john@test.com");
            resetToken.setExpirationDate(LocalDateTime.now().minusMinutes(1));

            ResetPasswordDTO dto = new ResetPasswordDTO("expired-token", "newpass123", "newpass123");

            when(passwordResetRepository.findByToken("expired-token"))
                    .thenReturn(Optional.of(resetToken));

            assertThatThrownBy(() -> authService.resetPassword(dto, "127.0.0.1"))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }
}
