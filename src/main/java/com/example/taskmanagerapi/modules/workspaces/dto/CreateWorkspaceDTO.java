package com.example.taskmanagerapi.modules.workspaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new workspace
 */
public record CreateWorkspaceDTO(
    @NotBlank(message = "Workspace name is required")
    @Size(min = 1, max = 100, message = "Workspace name must be between 1 and 100 characters")
    String name
) {}
