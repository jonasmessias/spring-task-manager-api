package com.example.taskmanagerapi.modules.workspaces.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.taskmanagerapi.modules.boards.dto.BoardResponseDTO;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;

/**
 * DTO for workspace detail response (includes boards)
 */
public record WorkspaceDetailDTO(
    String id,
    String name,
    List<BoardResponseDTO> boards,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public WorkspaceDetailDTO(Workspace workspace) {
        this(
            workspace.getId(),
            workspace.getName(),
            workspace.getBoards() != null 
                ? workspace.getBoards().stream()
                    .map(BoardResponseDTO::new)
                    .collect(Collectors.toList())
                : List.of(),
            workspace.getCreatedAt(),
            workspace.getUpdatedAt()
        );
    }
}
