package com.example.taskmanagerapi.modules.auth.controllers;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanagerapi.infra.security.TokenService;
import com.example.taskmanagerapi.modules.auth.domain.EmailVerificationToken;
import com.example.taskmanagerapi.modules.auth.domain.PasswordResetToken;
import com.example.taskmanagerapi.modules.auth.domain.RefreshToken;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.dto.AuthResponseDTO;
import com.example.taskmanagerapi.modules.auth.dto.ErrorResponseDTO;
import com.example.taskmanagerapi.modules.auth.dto.ForgotPasswordRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.LoginRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.RefreshTokenRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.RegisterRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.ResetPasswordDTO;
import com.example.taskmanagerapi.modules.auth.dto.VerifyEmailRequestDTO;
import com.example.taskmanagerapi.modules.auth.repositories.EmailVerificationRepository;
import com.example.taskmanagerapi.modules.auth.repositories.PasswordResetRepository;
import com.example.taskmanagerapi.modules.auth.repositories.UserRepository;
import com.example.taskmanagerapi.modules.auth.services.AuditLogService;
import com.example.taskmanagerapi.modules.auth.services.EmailService;
import com.example.taskmanagerapi.modules.auth.services.RefreshTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication and password management")
public class AuthController {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final PasswordResetRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final EmailVerificationRepository emailVerificationRepository;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @Operation(summary = "Login", description = "Authenticate user and return access token + refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<Object> login(
            @RequestBody LoginRequestDTO body,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {
        
        // Try to find user by email or username
        User user = this.repository.findByEmail(body.emailOrUsername())
                .or(() -> this.repository.findByUsername(body.emailOrUsername()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEmailVerified()) {
            return ResponseEntity.status(403).body(new ErrorResponseDTO(
                "EMAIL_NOT_VERIFIED",
                "Email not verified. Please check your inbox and verify your account."
            ));
        }

        if(passwordEncoder.matches(body.password(), user.getPassword())){
            String accessToken = tokenService.generateToken(user);
            String clientIp = getClientIp(request);
            
            // Create refresh token
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, clientIp, userAgent);
            
            // Audit log
            auditLogService.logLogin(user, clientIp, userAgent);
            
            return ResponseEntity.ok(new AuthResponseDTO(
                user.getName(), 
                accessToken, 
                refreshToken.getToken()
            ));
        }
        return ResponseEntity.badRequest().body(new ErrorResponseDTO(
            "INVALID_CREDENTIALS",
            "Invalid credentials."
        ));
    }

    @Operation(summary = "Register", description = "Create a new user account and return tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully registered",
                content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Email/username already registered or passwords don't match")
    })
    @PostMapping("/register")
    public ResponseEntity<Object> register(
            @RequestBody RegisterRequestDTO body,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {

        if(!body.password().equals(body.confirmPassword())){
            return ResponseEntity.badRequest().body("Passwords do not match");
        }

        Optional<User> existingEmail = this.repository.findByEmail(body.email());
        if(existingEmail.isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered");
        }
        
        Optional<User> existingUsername = this.repository.findByUsername(body.username());
        if(existingUsername.isPresent()) {
            return ResponseEntity.badRequest().body("Username already taken");
        }

        User newUser = new User();
        newUser.setPassword(passwordEncoder.encode(body.password()));
        newUser.setEmail(body.email());
        newUser.setName(body.name());
        newUser.setUsername(body.username());
        newUser.setEmailVerified(false);
        this.repository.save(newUser);

        // Generate email verification token (expires in 24 hours)
        String verificationToken = UUID.randomUUID().toString();
        emailVerificationRepository.deleteByEmail(body.email());

        EmailVerificationToken emailToken = new EmailVerificationToken();
        emailToken.setEmail(body.email());
        emailToken.setToken(verificationToken);
        emailToken.setExpirationDate(LocalDateTime.now().plusHours(24));
        emailVerificationRepository.save(emailToken);

        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;
        String message = "Olá, " + newUser.getName() + "!\n\n" +
                "Obrigado por se cadastrar no Task Manager!\n\n" +
                "Clique no link abaixo para verificar seu e-mail e ativar sua conta:\n" +
                verificationLink + "\n\n" +
                "O link expira em 24 horas.\n\n" +
                "Se você não criou esta conta, ignore este e-mail.\n\nTask Manager";

        emailService.sendEmail(body.email(), "Verifique seu e-mail - Task Manager", message);

        // Audit log
        String clientIp = getClientIp(request);
        auditLogService.logRegistration(newUser, clientIp);

        return ResponseEntity.ok("Registration successful! Please check your email to verify your account.");
    }

    @Operation(summary = "Refresh Token", description = "Get a new access token using refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully refreshed token",
                content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<Object> refreshToken(
            @RequestBody RefreshTokenRequestDTO body,
            HttpServletRequest request) {
        
        Optional<RefreshToken> refreshTokenOpt = refreshTokenService.validateRefreshToken(body.refreshToken());
        
        if (refreshTokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid or expired refresh token");
        }
        
        RefreshToken refreshToken = refreshTokenOpt.get();
        User user = refreshToken.getUser();
        
        // Generate new access token
        String newAccessToken = tokenService.generateToken(user);
        
        // Audit log
        auditLogService.logTokenRefresh(user, getClientIp(request));
        
        return ResponseEntity.ok(new AuthResponseDTO(
            user.getName(), 
            newAccessToken, 
            refreshToken.getToken()
        ));
    }

    @Operation(summary = "Logout", description = "Logout from current device by invalidating the specific refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully logged out from this device"),
        @ApiResponse(responseCode = "400", description = "Refresh token is required"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing access token")
    })
    @PostMapping("/logout")
    public ResponseEntity<Object> logout(
            @AuthenticationPrincipal User user,
            @RequestBody RefreshTokenRequestDTO body,
            HttpServletRequest request) {
        
        if (body == null || body.refreshToken() == null) {
            return ResponseEntity.badRequest().body("Refresh token is required for logout");
        }
        
        // Delete only the specific refresh token (per-instance logout)
        refreshTokenService.deleteRefreshToken(body.refreshToken());
        
        // Audit log
        auditLogService.logLogout(user, getClientIp(request));
        
        return ResponseEntity.ok("Logged out successfully from this device");
    }

    @Operation(summary = "Logout from All Devices", description = "Invalidate all refresh tokens for the user (logout everywhere)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully logged out from all devices"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @PostMapping("/logout-all")
    public ResponseEntity<Object> logoutAll(
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        
        // Delete all refresh tokens for this user
        refreshTokenService.deleteAllUserTokens(user);
        
        // Audit log with special warning level
        auditLogService.logLogoutAll(user, getClientIp(request));
        
        return ResponseEntity.ok("Logged out successfully from all devices");
    }

    @Operation(summary = "Verify Email", description = "Verify user email address using the token sent by email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Object> verifyEmail(
            @RequestBody VerifyEmailRequestDTO body,
            HttpServletRequest request) {

        Optional<EmailVerificationToken> tokenOpt = emailVerificationRepository.findByToken(body.token());

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid verification token.");
        }

        EmailVerificationToken verificationToken = tokenOpt.get();

        if (verificationToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            emailVerificationRepository.delete(verificationToken);
            return ResponseEntity.badRequest().body("Verification token has expired. Please register again.");
        }

        Optional<User> userOpt = repository.findByEmail(verificationToken.getEmail());

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found.");
        }

        User user = userOpt.get();
        user.setEmailVerified(true);
        repository.save(user);

        emailVerificationRepository.delete(verificationToken);

        auditLogService.logEmailVerification(user, getClientIp(request));

        return ResponseEntity.ok("Email verified successfully! You can now log in.");
    }

    @Operation(summary = "Resend Verification Email", description = "Resend the email verification link to the user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification email resent"),
        @ApiResponse(responseCode = "400", description = "Email not found or already verified")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Object> resendVerification(@RequestBody ForgotPasswordRequestDTO body) {

        Optional<User> userOpt = this.repository.findByEmail(body.email());

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(
                "EMAIL_NOT_FOUND",
                "E-mail not found."
            ));
        }

        User user = userOpt.get();

        if (user.isEmailVerified()) {
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(
                "EMAIL_ALREADY_VERIFIED",
                "This email is already verified. You can log in."
            ));
        }

        // Invalidate old token and generate a new one
        emailVerificationRepository.deleteByEmail(body.email());

        String verificationToken = UUID.randomUUID().toString();
        EmailVerificationToken emailToken = new EmailVerificationToken();
        emailToken.setEmail(body.email());
        emailToken.setToken(verificationToken);
        emailToken.setExpirationDate(LocalDateTime.now().plusHours(24));
        emailVerificationRepository.save(emailToken);

        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;
        String message = "Olá, " + user.getName() + "!\n\n" +
                "Você solicitou um novo link de verificação.\n\n" +
                "Clique no link abaixo para verificar seu e-mail e ativar sua conta:\n" +
                verificationLink + "\n\n" +
                "O link expira em 24 horas.\n\n" +
                "Se você não solicitou isso, ignore este e-mail.\n\nTask Manager";

        emailService.sendEmail(body.email(), "Novo link de verificação - Task Manager", message);

        return ResponseEntity.ok("Verification email resent to: " + body.email());
    }

    @Operation(summary = "Forgot Password", description = "Send password reset email to user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reset email sent successfully"),
        @ApiResponse(responseCode = "400", description = "Email not found")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Object> forgotPassword(@RequestBody ForgotPasswordRequestDTO body) {

        Optional<User> userOpt = this.repository.findByEmail(body.email());

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("E-mail not found.");
        }

        String token = UUID.randomUUID().toString();
        passwordResetTokenRepository.deleteByEmail(body.email());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(body.email());
        resetToken.setToken(token);
        resetToken.setExpirationDate(LocalDateTime.now().plusMinutes(30));
        passwordResetTokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String message = "Olá, " + userOpt.get().getName() + "!\n\n" +
                "Clique no link abaixo para redefinir sua senha:\n" +
                resetLink + "\n\n" +
                "O link expira em 30 minutos.\n\nTask Manager";

        emailService.sendEmail(body.email(), "Redefinição de senha - Task Manager", message);

        return ResponseEntity.ok("E-mail de redefinição enviado para: " + body.email());
    }

    @Operation(summary = "Reset Password", description = "Reset user password using token from email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid/expired token or passwords don't match")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Object> resetPassword(
            @RequestBody ResetPasswordDTO body,
            HttpServletRequest request) {

        if(!body.newPassword().equals(body.confirmNewPassword())) {
            return ResponseEntity.badRequest().body("Passwords do not match");
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(body.token());

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid token.");
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Expired token.");
        }

        Optional<User> userOpt = repository.findByEmail(resetToken.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found.");
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(body.newPassword()));
        repository.save(user);
        
        // CRITICAL SECURITY: Invalidate all refresh tokens when password is reset
        // This prevents attackers from continuing to use stolen tokens
        refreshTokenService.deleteAllUserTokens(user);
        
        // Audit log for security tracking
        auditLogService.logPasswordReset(user, getClientIp(request));
        
        passwordResetTokenRepository.delete(resetToken);

        return ResponseEntity.ok("Password reset complete! All sessions have been logged out for security.");
    }
}
