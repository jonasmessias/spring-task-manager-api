package com.example.taskmanagerapi.modules.lists.dto;

import java.util.List;

import com.example.taskmanagerapi.modules.cards.dto.CardResponseDTO;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ListResponseDTO - Data Transfer Object for list responses
 */
@Schema(description = "Response object representing a list")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ListResponseDTO(
    @Schema(description = "List ID", example = "550e8400-e29b-41d4-a716-446655440000")
    String id,
    
    @Schema(description = "Name of the list", example = "To Do")
    String name,
    
    @Schema(description = "Position of the list in the board", example = "0")
    Integer position,
    
    @Schema(description = "ID of the board this list belongs to", example = "550e8400-e29b-41d4-a716-446655440001")
    String boardId,

    @Schema(description = "Cards within this list (only included in board detail responses)")
    List<CardResponseDTO> cards
) {
    /**
     * Constructor from BoardList entity (without cards — used for simple list responses)
     */
    public ListResponseDTO(BoardList list) {
        this(
            list.getId(),
            list.getName(),
            list.getPosition(),
            list.getBoard().getId(),
            null
        );
    }

    /**
     * Constructor from BoardList entity with cards — used inside BoardDetailDTO
     */
    public static ListResponseDTO withCards(BoardList list) {
        return new ListResponseDTO(
            list.getId(),
            list.getName(),
            list.getPosition(),
            list.getBoard().getId(),
            list.getCards() != null
                ? list.getCards().stream()
                    .sorted((a, b) -> {
                        Integer posA = a.getPosition();
                        Integer posB = b.getPosition();
                        if (posA == null && posB == null) return 0;
                        if (posA == null) return 1;
                        if (posB == null) return -1;
                        return posA.compareTo(posB);
                    })
                    .map(CardResponseDTO::new)
                    .toList()
                : List.of()
        );
    }
}
