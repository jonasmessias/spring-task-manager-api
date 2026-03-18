package com.example.taskmanagerapi.modules.auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.dto.AuthResponseDTO;
import com.example.taskmanagerapi.modules.auth.dto.ErrorResponseDTO;
import com.example.taskmanagerapi.modules.auth.dto.ForgotPasswordRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.GoogleTokenRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.LoginRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.MessageResponseDTO;
import com.example.taskmanagerapi.modules.auth.dto.RefreshTokenRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.RegisterRequestDTO;
import com.example.taskmanagerapi.modules.auth.dto.ResetPasswordDTO;
import com.example.taskmanagerapi.modules.auth.dto.VerifyEmailRequestDTO;
import com.example.taskmanagerapi.modules.auth.services.AuthService;
import com.example.taskmanagerapi.modules.auth.services.AuthService.LoginResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * AuthController — thin REST controller that delegates all business logic
 * to AuthService. Responsible only for HTTP concerns (request/response mapping).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication and password management")
public class AuthController {

    private final AuthService authService;

    // =========================================================================
    // Helpers
    // =========================================================================

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private ResponseEntity<Object> toResponse(LoginResult result) {
        return ResponseEntity.status(result.status()).body(result.body());
    }

    // =========================================================================
    // Endpoints
    // =========================================================================

    @Operation(summary = "Login", description = "Authenticate user and return access token + refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials — `INVALID_CREDENTIALS`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Email not verified — `EMAIL_NOT_VERIFIED`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<Object> login(
            @RequestBody LoginRequestDTO body,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {
        return toResponse(authService.login(body, getClientIp(request), userAgent));
    }

    @Operation(summary = "Google Login / Register",
            description = "Authenticate or create an account using a Google ID token obtained from Google Sign-In on the frontend.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authenticated successfully",
                content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired Google token — `INVALID_GOOGLE_TOKEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/google")
    public ResponseEntity<Object> googleLogin(
            @Valid @RequestBody GoogleTokenRequestDTO body,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {
        return toResponse(authService.googleLogin(body.idToken(), getClientIp(request), userAgent));
    }

    @Operation(summary = "Register", description = "Create a new user account. Sends a verification email on success.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Account created — verification email sent",
                content = @Content(schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation error — `PASSWORDS_DO_NOT_MATCH`, `EMAIL_ALREADY_EXISTS`, `USERNAME_ALREADY_EXISTS`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Email service error — `EMAIL_SEND_ERROR`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<Object> register(
            @Valid @RequestBody RegisterRequestDTO body,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {
        return toResponse(authService.register(body, getClientIp(request)));
    }

    @Operation(summary = "Refresh Token", description = "Get a new access token using refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully refreshed token",
                content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token — `INVALID_TOKEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<Object> refreshToken(
            @RequestBody RefreshTokenRequestDTO body,
            HttpServletRequest request) {
        return toResponse(authService.refreshToken(body.refreshToken(), getClientIp(request)));
    }

    @Operation(summary = "Logout", description = "Logout from current device by invalidating the specific refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully logged out from this device",
                content = @Content(schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Refresh token is required — `MISSING_REFRESH_TOKEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing access token")
    })
    @PostMapping("/logout")
    public ResponseEntity<Object> logout(
            @AuthenticationPrincipal User user,
            @RequestBody RefreshTokenRequestDTO body,
            HttpServletRequest request) {
        if (body == null || body.refreshToken() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(
                "MISSING_REFRESH_TOKEN", "Refresh token is required for logout.", 400
            ));
        }
        authService.logout(user, body.refreshToken(), getClientIp(request));
        return ResponseEntity.ok(new MessageResponseDTO("Logged out successfully from this device."));
    }

    @Operation(summary = "Logout from All Devices", description = "Invalidate all refresh tokens for the user (logout everywhere)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully logged out from all devices",
                content = @Content(schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @PostMapping("/logout-all")
    public ResponseEntity<Object> logoutAll(
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        authService.logoutAll(user, getClientIp(request));
        return ResponseEntity.ok(new MessageResponseDTO("Logged out successfully from all devices."));
    }

    @Operation(summary = "Verify Email", description = "Verify user email address using the token sent by email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email verified successfully",
                content = @Content(schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Token error — `INVALID_TOKEN`, `EXPIRED_TOKEN`, `EMAIL_ALREADY_VERIFIED`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Object> verifyEmail(
            @RequestBody VerifyEmailRequestDTO body,
            HttpServletRequest request) {
        return toResponse(authService.verifyEmail(body.token(), getClientIp(request)));
    }

    @Operation(summary = "Resend Verification Email", description = "Resend the email verification link to the user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification email resent",
                content = @Content(schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Email already verified — `EMAIL_ALREADY_VERIFIED`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Email not found — `EMAIL_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Email service error — `EMAIL_SEND_ERROR`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Object> resendVerification(@RequestBody ForgotPasswordRequestDTO body) {
        return toResponse(authService.resendVerification(body.email()));
    }

    @Operation(summary = "Forgot Password", description = "Send password reset email to user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reset email sent successfully",
                content = @Content(schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Email not found — `EMAIL_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Email service error — `EMAIL_SEND_ERROR`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Object> forgotPassword(@RequestBody ForgotPasswordRequestDTO body) {
        return toResponse(authService.forgotPassword(body.email()));
    }

    @Operation(summary = "Reset Password", description = "Reset user password using token from email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successfully",
                content = @Content(schema = @Schema(implementation = MessageResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Passwords don't match — `PASSWORDS_DO_NOT_MATCH`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token — `INVALID_TOKEN`, `EXPIRED_TOKEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Object> resetPassword(
            @RequestBody ResetPasswordDTO body,
            HttpServletRequest request) {
        return toResponse(authService.resetPassword(body, getClientIp(request)));
    }
}
