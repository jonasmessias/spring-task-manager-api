package com.example.taskmanagerapi.modules.workspaces.dto;

import java.time.LocalDateTime;

import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;

/**
 * DTO for workspace response (without boards)
 */
public record WorkspaceResponseDTO(
    String id,
    String name,
    Integer boardCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public WorkspaceResponseDTO(Workspace workspace) {
        this(
            workspace.getId(),
            workspace.getName(),
            workspace.getBoards() != null ? workspace.getBoards().size() : 0,
            workspace.getCreatedAt(),
            workspace.getUpdatedAt()
        );
    }
}
