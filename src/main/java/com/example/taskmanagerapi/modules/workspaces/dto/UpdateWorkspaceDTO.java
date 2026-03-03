package com.example.taskmanagerapi.modules.workspaces.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for updating an existing workspace
 */
public record UpdateWorkspaceDTO(
    @Size(min = 1, max = 100, message = "Workspace name must be between 1 and 100 characters")
    String name
) {}
