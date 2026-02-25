package com.example.taskmanagerapi.controllers;

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

import com.example.taskmanagerapi.domain.passwordreset.PasswordResetToken;
import com.example.taskmanagerapi.domain.refreshtoken.RefreshToken;
import com.example.taskmanagerapi.domain.user.User;
import com.example.taskmanagerapi.dto.AuthResponseDTO;
import com.example.taskmanagerapi.dto.ForgotPasswordRequestDTO;
import com.example.taskmanagerapi.dto.LoginRequestDTO;
import com.example.taskmanagerapi.dto.RefreshTokenRequestDTO;
import com.example.taskmanagerapi.dto.RegisterRequestDTO;
import com.example.taskmanagerapi.dto.ResetPasswordDTO;
import com.example.taskmanagerapi.infra.security.TokenService;
import com.example.taskmanagerapi.repositories.PasswordResetRepository;
import com.example.taskmanagerapi.repositories.UserRepository;
import com.example.taskmanagerapi.services.AuditLogService;
import com.example.taskmanagerapi.services.EmailService;
import com.example.taskmanagerapi.services.RefreshTokenService;

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
        
        User user = this.repository.findByEmail(body.email())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
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
        return ResponseEntity.badRequest().body("Invalid credentials");
    }

    @Operation(summary = "Register", description = "Create a new user account and return tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully registered",
                content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Email already registered or passwords don't match")
    })
    @PostMapping("/register")
    public ResponseEntity<Object> register(
            @RequestBody RegisterRequestDTO body,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {

        if(!body.password().equals(body.confirmPassword())){
            return ResponseEntity.badRequest().body("Passwords do not match");
        }

        Optional<User> user = this.repository.findByEmail(body.email());

        if(user.isEmpty()) {
            User newUser = new User();
            newUser.setPassword(passwordEncoder.encode(body.password()));
            newUser.setEmail(body.email());
            newUser.setName(body.name());
            this.repository.save(newUser);

            String accessToken = tokenService.generateToken(newUser);
            String clientIp = getClientIp(request);
            
            // Create refresh token
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(newUser, clientIp, userAgent);
            
            // Audit log
            auditLogService.logRegistration(newUser, clientIp);
            
            return ResponseEntity.ok(new AuthResponseDTO(
                newUser.getName(), 
                accessToken, 
                refreshToken.getToken()
            ));
        }
        return ResponseEntity.badRequest().body("Email already registered");
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
