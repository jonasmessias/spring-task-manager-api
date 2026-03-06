package com.example.taskmanagerapi.modules.boards.dto;

import com.example.taskmanagerapi.modules.boards.domain.BoardType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating a new board")
public record CreateBoardDTO(
    @NotBlank(message = "Board name is required")
    @Size(min = 1, max = 100, message = "Board name must be between 1 and 100 characters")
    @Schema(description = "Name of the board", example = "Sprint Board", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @Schema(description = "Type of the board. Defaults to BOARD", example = "KANBAN")
    BoardType type,

    @Size(max = 500, message = "Description must be at most 500 characters")
    @Schema(description = "Optional description for the board", example = "Sprint 1 tasks")
    String description
) {}
