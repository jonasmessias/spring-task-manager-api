package com.example.taskmanagerapi.modules.auth.services;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.infra.security.TokenService;
import com.example.taskmanagerapi.modules.auth.domain.EmailVerificationToken;
import com.example.taskmanagerapi.modules.auth.domain.PasswordResetToken;
import com.example.taskmanagerapi.modules.auth.domain.RefreshToken;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.dto.AuthResponseDTO;
import com.example.taskmanagerapi.modules.auth.dto.ErrorResponseDTO;
import com.example.taskmanagerapi.modules.auth.dto.LoginRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.MessageResponseDTO;
import com.example.taskmanagerapi.modules.auth.dto.RegisterRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.ResetPasswordDTO;
import com.example.taskmanagerapi.modules.auth.repositories.EmailVerificationRepository;
import com.example.taskmanagerapi.modules.auth.repositories.PasswordResetRepository;
import com.example.taskmanagerapi.modules.auth.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * AuthService — contains all authentication business logic,
 * extracted from AuthController to follow Single Responsibility Principle.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final GoogleAuthService googleAuthService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // =========================================================================
    // Login
    // =========================================================================

    /**
     * Authenticate a user by email/username + password.
     * Returns either an AuthResponseDTO or ErrorResponseDTO.
     */
    public record LoginResult(Object body, int status) {}

    public LoginResult login(LoginRequestDTO dto, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(dto.emailOrUsername())
                .or(() -> userRepository.findByUsername(dto.emailOrUsername()))
                .orElse(null);

        // OAuth-only users have no password
        if (user != null && user.getPassword() == null) {
            return new LoginResult(new ErrorResponseDTO(
                "USE_GOOGLE_LOGIN",
                "This account uses Google Sign-In. Please log in with Google.",
                401
            ), 401);
        }

        if (user == null || !passwordEncoder.matches(dto.password(), user.getPassword())) {
            return new LoginResult(new ErrorResponseDTO(
                "INVALID_CREDENTIALS", "Invalid credentials.", 401
            ), 401);
        }

        if (!user.isEmailVerified()) {
            return new LoginResult(new ErrorResponseDTO(
                "EMAIL_NOT_VERIFIED",
                "Email not verified. Please check your inbox and verify your account.",
                403
            ), 403);
        }

        String accessToken = tokenService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, ipAddress, userAgent);
        auditLogService.logLogin(user, ipAddress, userAgent);

        return new LoginResult(
            new AuthResponseDTO(user.getName(), accessToken, refreshToken.getToken()), 200
        );
    }

    // =========================================================================
    // Google Login
    // =========================================================================

    public LoginResult googleLogin(String idToken, String ipAddress, String userAgent) {
        try {
            User user = googleAuthService.verifyAndGetUser(idToken);
            String accessToken = tokenService.generateToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, ipAddress, userAgent);
            auditLogService.logLogin(user, ipAddress, userAgent);
            return new LoginResult(
                new AuthResponseDTO(user.getName(), accessToken, refreshToken.getToken()), 200
            );
        } catch (IllegalArgumentException e) {
            return new LoginResult(new ErrorResponseDTO(
                "INVALID_GOOGLE_TOKEN", "Invalid or expired Google token.", 401
            ), 401);
        }
    }

    // =========================================================================
    // Register
    // =========================================================================

    @Transactional
    public LoginResult register(RegisterRequestDTO dto, String ipAddress) {
        if (!dto.password().equals(dto.confirmPassword())) {
            return new LoginResult(new ErrorResponseDTO(
                "PASSWORDS_DO_NOT_MATCH", "Passwords do not match.", 400
            ), 400);
        }

        if (userRepository.findByEmail(dto.email()).isPresent()) {
            return new LoginResult(new ErrorResponseDTO(
                "EMAIL_ALREADY_EXISTS", "Email already registered.", 400
            ), 400);
        }

        if (userRepository.findByUsername(dto.username()).isPresent()) {
            return new LoginResult(new ErrorResponseDTO(
                "USERNAME_ALREADY_EXISTS", "Username already taken.", 400
            ), 400);
        }

        User newUser = new User();
        newUser.setPassword(passwordEncoder.encode(dto.password()));
        newUser.setEmail(dto.email());
        newUser.setName(dto.name());
        newUser.setUsername(dto.username());
        newUser.setEmailVerified(false);
        userRepository.save(newUser);

        sendVerificationEmail(newUser);
        auditLogService.logRegistration(newUser, ipAddress);

        return new LoginResult(new MessageResponseDTO(
            "Registration successful! Please check your email to verify your account."
        ), 201);
    }

    // =========================================================================
    // Refresh Token
    // =========================================================================

    public LoginResult refreshToken(String refreshTokenStr, String ipAddress) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenService.validateRefreshToken(refreshTokenStr);

        if (refreshTokenOpt.isEmpty()) {
            return new LoginResult(new ErrorResponseDTO(
                "INVALID_TOKEN", "Invalid or expired refresh token.", 401
            ), 401);
        }

        RefreshToken refreshToken = refreshTokenOpt.get();
        User user = refreshToken.getUser();
        String newAccessToken = tokenService.generateToken(user);
        auditLogService.logTokenRefresh(user, ipAddress);

        return new LoginResult(
            new AuthResponseDTO(user.getName(), newAccessToken, refreshToken.getToken()), 200
        );
    }

    // =========================================================================
    // Logout
    // =========================================================================

    public void logout(User user, String refreshTokenStr, String ipAddress) {
        refreshTokenService.deleteRefreshToken(refreshTokenStr);
        auditLogService.logLogout(user, ipAddress);
    }

    public void logoutAll(User user, String ipAddress) {
        refreshTokenService.deleteAllUserTokens(user);
        auditLogService.logLogoutAll(user, ipAddress);
    }

    // =========================================================================
    // Email Verification
    // =========================================================================

    @Transactional
    public LoginResult verifyEmail(String token, String ipAddress) {
        Optional<EmailVerificationToken> tokenOpt = emailVerificationRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return new LoginResult(new ErrorResponseDTO(
                "INVALID_TOKEN", "Invalid verification token.", 400
            ), 400);
        }

        EmailVerificationToken verificationToken = tokenOpt.get();

        if (verificationToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            emailVerificationRepository.delete(verificationToken);
            return new LoginResult(new ErrorResponseDTO(
                "EXPIRED_TOKEN", "Verification token has expired. Please request a new one.", 400
            ), 400);
        }

        Optional<User> userOpt = userRepository.findByEmail(verificationToken.getEmail());
        if (userOpt.isEmpty()) {
            return new LoginResult(new ErrorResponseDTO(
                "INVALID_TOKEN", "User not found for this token.", 400
            ), 400);
        }

        User user = userOpt.get();

        if (user.isEmailVerified()) {
            emailVerificationRepository.delete(verificationToken);
            return new LoginResult(new ErrorResponseDTO(
                "EMAIL_ALREADY_VERIFIED", "This email is already verified. You can log in.", 400
            ), 400);
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        emailVerificationRepository.delete(verificationToken);
        auditLogService.logEmailVerification(user, ipAddress);

        return new LoginResult(new MessageResponseDTO(
            "Email verified successfully! You can now log in."
        ), 200);
    }

    @Transactional
    public LoginResult resendVerification(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return new LoginResult(new ErrorResponseDTO(
                "EMAIL_NOT_FOUND", "E-mail not found.", 404
            ), 404);
        }

        User user = userOpt.get();

        if (user.isEmailVerified()) {
            return new LoginResult(new ErrorResponseDTO(
                "EMAIL_ALREADY_VERIFIED", "This email is already verified. You can log in.", 400
            ), 400);
        }

        sendVerificationEmail(user);

        return new LoginResult(new MessageResponseDTO(
            "Verification email resent successfully."
        ), 200);
    }

    // =========================================================================
    // Password Reset
    // =========================================================================

    @Transactional
    public LoginResult forgotPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return new LoginResult(new ErrorResponseDTO(
                "EMAIL_NOT_FOUND", "E-mail not found.", 404
            ), 404);
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        passwordResetRepository.deleteByEmail(email);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(email);
        resetToken.setToken(token);
        resetToken.setExpirationDate(LocalDateTime.now().plusMinutes(30));
        passwordResetRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        emailService.sendHtmlEmail(email, "Password Reset - Task Manager", "password-reset", Map.of(
            "userName", user.getName(),
            "resetLink", resetLink
        ));

        return new LoginResult(new MessageResponseDTO(
            "Password reset email sent. Please check your inbox."
        ), 200);
    }

    @Transactional
    public LoginResult resetPassword(ResetPasswordDTO dto, String ipAddress) {
        if (!dto.newPassword().equals(dto.confirmNewPassword())) {
            return new LoginResult(new ErrorResponseDTO(
                "PASSWORDS_DO_NOT_MATCH", "Passwords do not match.", 400
            ), 400);
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetRepository.findByToken(dto.token());
        if (tokenOpt.isEmpty()) {
            return new LoginResult(new ErrorResponseDTO(
                "INVALID_TOKEN", "Invalid password reset token.", 401
            ), 401);
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            passwordResetRepository.delete(resetToken);
            return new LoginResult(new ErrorResponseDTO(
                "EXPIRED_TOKEN", "Password reset token has expired. Please request a new one.", 401
            ), 401);
        }

        Optional<User> userOpt = userRepository.findByEmail(resetToken.getEmail());
        if (userOpt.isEmpty()) {
            return new LoginResult(new ErrorResponseDTO(
                "INVALID_TOKEN", "Invalid password reset token.", 401
            ), 401);
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        // CRITICAL SECURITY: Invalidate all refresh tokens on password reset
        refreshTokenService.deleteAllUserTokens(user);
        auditLogService.logPasswordReset(user, ipAddress);
        passwordResetRepository.delete(resetToken);

        return new LoginResult(new MessageResponseDTO(
            "Password reset successfully. All sessions have been logged out for security."
        ), 200);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void sendVerificationEmail(User user) {
        emailVerificationRepository.deleteByEmail(user.getEmail());

        String verificationToken = UUID.randomUUID().toString();
        EmailVerificationToken emailToken = new EmailVerificationToken();
        emailToken.setEmail(user.getEmail());
        emailToken.setToken(verificationToken);
        emailToken.setExpirationDate(LocalDateTime.now().plusHours(24));
        emailVerificationRepository.save(emailToken);

        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

        emailService.sendHtmlEmail(user.getEmail(), "Verify your email - Task Manager", "email-verification", Map.of(
            "userName", user.getName(),
            "verificationLink", verificationLink
        ));
    }
}
