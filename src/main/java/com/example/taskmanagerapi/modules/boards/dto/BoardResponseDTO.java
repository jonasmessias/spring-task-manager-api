package com.example.taskmanagerapi.modules.boards.dto;

import java.time.LocalDateTime;

import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.domain.BoardType;

public record BoardResponseDTO(
    String id,
    String name,
    BoardType type,
    String description,
    String ownerId,
    String ownerName,
    int listsCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public BoardResponseDTO(Board board) {
        this(
            board.getId(),
            board.getName(),
            board.getType(),
            board.getDescription(),
            board.getOwner().getId(),
            board.getOwner().getName(),
            board.getLists() != null ? board.getLists().size() : 0,
            board.getCreatedAt(),
            board.getUpdatedAt()
        );
    }
}
