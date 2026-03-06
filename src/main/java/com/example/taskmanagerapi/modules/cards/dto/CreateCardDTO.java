package com.example.taskmanagerapi.modules.cards.dto;

import com.example.taskmanagerapi.modules.cards.domain.CardStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating a new card")
public record CreateCardDTO(
    @NotBlank(message = "Card name is required")
    @Size(min = 1, max = 255, message = "Card name must be between 1 and 255 characters")
    @Schema(description = "Name of the card", example = "Implement login page", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    @Schema(description = "Optional description of the card", example = "Build the login form with validation")
    String description,

    @Schema(description = "Initial status of the card. Defaults to ACTIVE", example = "ACTIVE")
    CardStatus status,

    @Min(value = 0, message = "Position must be zero or greater")
    @Schema(description = "Position within the list (0-indexed). Auto-appended at end if omitted.", example = "0")
    Integer position
) {}
