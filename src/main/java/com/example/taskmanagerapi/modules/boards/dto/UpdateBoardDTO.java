package com.example.taskmanagerapi.modules.boards.dto;

import com.example.taskmanagerapi.modules.boards.domain.BoardType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating a board. All fields are optional.")
public record UpdateBoardDTO(
    @Size(min = 1, max = 100, message = "Board name must be between 1 and 100 characters")
    @Schema(description = "New name for the board", example = "Sprint Board")
    String name,

    @Schema(description = "New type for the board", example = "KANBAN")
    BoardType type,

    @Size(max = 500, message = "Description must be at most 500 characters")
    @Schema(description = "New description for the board", example = "Sprint 1 tasks")
    String description
) {}
