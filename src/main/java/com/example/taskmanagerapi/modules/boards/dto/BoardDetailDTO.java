package com.example.taskmanagerapi.modules.boards.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.domain.BoardType;
import com.example.taskmanagerapi.modules.lists.dto.ListResponseDTO;

public record BoardDetailDTO(
    String id,
    String name,
    BoardType type,
    String description,
    String ownerId,
    String ownerName,
    List<ListResponseDTO> lists,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public BoardDetailDTO(Board board) {
        this(
            board.getId(),
            board.getName(),
            board.getType(),
            board.getDescription(),
            board.getOwner().getId(),
            board.getOwner().getName(),
            board.getLists() != null ? 
                board.getLists().stream().map(ListResponseDTO::new).toList() : 
                List.of(),
            board.getCreatedAt(),
            board.getUpdatedAt()
        );
    }
}
