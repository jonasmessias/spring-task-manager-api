package com.example.taskmanagerapi.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * UpdateProfileDTO - Data Transfer Object for updating the current user's profile
 */
@Schema(description = "Request body for updating user profile")
public record UpdateProfileDTO(
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    @Schema(description = "New display name", example = "John Doe")
    String name,

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Schema(description = "New username", example = "johndoe")
    String username
) {}
