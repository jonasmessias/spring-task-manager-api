package com.example.taskmanagerapi.modules.cards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * MoveCardDTO - Data Transfer Object for moving a card to another list
 */
@Schema(description = "Request body for moving a card to a different list")
public record MoveCardDTO(
    @NotBlank(message = "Target list ID is required")
    @Schema(description = "ID of the destination list", example = "uuid-of-target-list", requiredMode = Schema.RequiredMode.REQUIRED)
    String targetListId,

    @Min(value = 0, message = "Position must be zero or greater")
    @Schema(description = "Position in the destination list (0-indexed). If omitted, card is appended at the end.", example = "0")
    Integer position
) {}
