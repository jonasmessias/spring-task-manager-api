package com.example.taskmanagerapi.modules.auth.services;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

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

    public AuthResponseDTO login(LoginRequestDTO dto, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(dto.emailOrUsername())
                .or(() -> userRepository.findByUsername(dto.emailOrUsername()))
                .orElse(null);

        if (user != null && user.getPassword() == null) {
            throw new UnauthorizedException("USE_GOOGLE_LOGIN",
                    "This account uses Google Sign-In. Please log in with Google.");
        }

        if (user == null || !passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials.");
        }

        if (!user.isEmailVerified()) {
            throw new ForbiddenException("EMAIL_NOT_VERIFIED",
                    "Email not verified. Please check your inbox and verify your account.");
        }

        String accessToken = tokenService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, ipAddress, userAgent);
        auditLogService.logLogin(user, ipAddress, userAgent);

        return new AuthResponseDTO(user.getName(), accessToken, refreshToken.getToken());
    }

    public AuthResponseDTO googleLogin(String idToken, String ipAddress, String userAgent) {
        try {
            User user = googleAuthService.verifyAndGetUser(idToken);
            String accessToken = tokenService.generateToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, ipAddress, userAgent);
            auditLogService.logLogin(user, ipAddress, userAgent);
            return new AuthResponseDTO(user.getName(), accessToken, refreshToken.getToken());
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("INVALID_GOOGLE_TOKEN", "Invalid or expired Google token.");
        }
    }

    @Transactional
    public MessageResponseDTO register(RegisterRequestDTO dto, String ipAddress) {
        if (!dto.password().equals(dto.confirmPassword())) {
            throw new BusinessException("PASSWORDS_DO_NOT_MATCH", "Passwords do not match.");
        }

        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email already registered.");
        }

        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new BusinessException("USERNAME_ALREADY_EXISTS", "Username already taken.");
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

        return new MessageResponseDTO(
            "Registration successful! Please check your email to verify your account."
        );
    }

    public AuthResponseDTO refreshToken(String refreshTokenStr, String ipAddress) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenStr)
                .orElseThrow(() -> new UnauthorizedException("INVALID_TOKEN",
                        "Invalid or expired refresh token."));

        User user = refreshToken.getUser();
        String newAccessToken = tokenService.generateToken(user);
        auditLogService.logTokenRefresh(user, ipAddress);

        return new AuthResponseDTO(user.getName(), newAccessToken, refreshToken.getToken());
    }

    public void logout(User user, String refreshTokenStr, String ipAddress) {
        refreshTokenService.deleteRefreshToken(refreshTokenStr);
        auditLogService.logLogout(user, ipAddress);
    }

    public void logoutAll(User user, String ipAddress) {
        refreshTokenService.deleteAllUserTokens(user);
        auditLogService.logLogoutAll(user, ipAddress);
    }

    @Transactional
    public MessageResponseDTO verifyEmail(String token, String ipAddress) {
        EmailVerificationToken verificationToken = emailVerificationRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "Invalid verification token."));

        if (verificationToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            emailVerificationRepository.delete(verificationToken);
            throw new BusinessException("EXPIRED_TOKEN",
                    "Verification token has expired. Please request a new one.");
        }

        User user = userRepository.findByEmail(verificationToken.getEmail())
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN",
                        "User not found for this token."));

        if (user.isEmailVerified()) {
            emailVerificationRepository.delete(verificationToken);
            throw new BusinessException("EMAIL_ALREADY_VERIFIED",
                    "This email is already verified. You can log in.");
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        emailVerificationRepository.delete(verificationToken);
        auditLogService.logEmailVerification(user, ipAddress);

        return new MessageResponseDTO("Email verified successfully! You can now log in.");
    }

    @Transactional
    public MessageResponseDTO resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("EMAIL_NOT_FOUND", "E-mail not found."));

        if (user.isEmailVerified()) {
            throw new BusinessException("EMAIL_ALREADY_VERIFIED",
                    "This email is already verified. You can log in.");
        }

        sendVerificationEmail(user);

        return new MessageResponseDTO("Verification email resent successfully.");
    }

    @Transactional
    public MessageResponseDTO forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("EMAIL_NOT_FOUND", "E-mail not found."));

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

        return new MessageResponseDTO("Password reset email sent. Please check your inbox.");
    }

    @Transactional
    public MessageResponseDTO resetPassword(ResetPasswordDTO dto, String ipAddress) {
        if (!dto.newPassword().equals(dto.confirmNewPassword())) {
            throw new BusinessException("PASSWORDS_DO_NOT_MATCH", "Passwords do not match.");
        }

        PasswordResetToken resetToken = passwordResetRepository.findByToken(dto.token())
                .orElseThrow(() -> new UnauthorizedException("INVALID_TOKEN",
                        "Invalid password reset token."));

        if (resetToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            passwordResetRepository.delete(resetToken);
            throw new UnauthorizedException("EXPIRED_TOKEN",
                    "Password reset token has expired. Please request a new one.");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new UnauthorizedException("INVALID_TOKEN",
                        "Invalid password reset token."));

        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        refreshTokenService.deleteAllUserTokens(user);
        auditLogService.logPasswordReset(user, ipAddress);
        passwordResetRepository.delete(resetToken);

        return new MessageResponseDTO(
            "Password reset successfully. All sessions have been logged out for security."
        );
    }

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
