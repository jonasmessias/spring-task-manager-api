package com.example.taskmanagerapi.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for user registration")
public record RegisterRequestDTO(
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    @Schema(description = "Full display name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "Unique username", example = "johndoe", requiredMode = Schema.RequiredMode.REQUIRED)
    String username,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Schema(description = "Email address", example = "john@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Password (min 6 characters)", example = "secret123", requiredMode = Schema.RequiredMode.REQUIRED)
    String password,

    @NotBlank(message = "Password confirmation is required")
    @Schema(description = "Must match password", example = "secret123", requiredMode = Schema.RequiredMode.REQUIRED)
    String confirmPassword
) {}
