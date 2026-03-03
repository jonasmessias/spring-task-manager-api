package com.example.taskmanagerapi.modules.lists.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * CreateListDTO - Data Transfer Object for creating a new list
 */
@Schema(description = "Request body for creating a new list")
public record CreateListDTO(
    @NotBlank(message = "List name is required")
    @Schema(description = "Name of the list", example = "To Do", requiredMode = Schema.RequiredMode.REQUIRED)
    String name
) {}
