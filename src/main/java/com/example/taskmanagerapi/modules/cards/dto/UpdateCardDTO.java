package com.example.taskmanagerapi.modules.cards.dto;

import com.example.taskmanagerapi.modules.cards.domain.CardStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating a card. All fields are optional.")
public record UpdateCardDTO(
    @Size(min = 1, max = 255, message = "Card name must be between 1 and 255 characters")
    @Schema(description = "New name for the card", example = "Implement login page")
    String name,

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    @Schema(description = "New description for the card", example = "Build the login form with validation")
    String description,

    @Schema(description = "New status for the card", example = "COMPLETED")
    CardStatus status,

    @Min(value = 0, message = "Position must be zero or greater")
    @Schema(description = "New position within the same list (0-indexed)", example = "2")
    Integer position
) {}
