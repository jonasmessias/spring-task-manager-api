package com.example.taskmanagerapi.modules.lists.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * UpdateListDTO - Data Transfer Object for updating a list
 */
@Schema(description = "Request body for updating a list")
public record UpdateListDTO(
    @Schema(description = "New name for the list", example = "In Progress")
    String name,
    
    @Schema(description = "New position for the list", example = "1")
    Integer position
) {}
