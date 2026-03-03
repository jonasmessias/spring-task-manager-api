package com.example.taskmanagerapi.modules.lists.dto;

import com.example.taskmanagerapi.modules.lists.domain.BoardList;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ListResponseDTO - Data Transfer Object for list responses
 */
@Schema(description = "Response object representing a list")
public record ListResponseDTO(
    @Schema(description = "List ID", example = "550e8400-e29b-41d4-a716-446655440000")
    String id,
    
    @Schema(description = "Name of the list", example = "To Do")
    String name,
    
    @Schema(description = "Position of the list in the board", example = "0")
    Integer position,
    
    @Schema(description = "ID of the board this list belongs to", example = "550e8400-e29b-41d4-a716-446655440001")
    String boardId
) {
    /**
     * Constructor from BoardList entity
     */
    public ListResponseDTO(BoardList list) {
        this(
            list.getId(),
            list.getName(),
            list.getPosition(),
            list.getBoard().getId()
        );
    }
}
