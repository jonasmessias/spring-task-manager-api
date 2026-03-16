package com.example.taskmanagerapi.modules.cards.controllers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.dto.ErrorResponseDTO;
import com.example.taskmanagerapi.modules.boards.services.BoardMemberService;
import com.example.taskmanagerapi.modules.cards.domain.Card;
import com.example.taskmanagerapi.modules.cards.dto.CardResponseDTO;
import com.example.taskmanagerapi.modules.cards.dto.CreateCardDTO;
import com.example.taskmanagerapi.modules.cards.dto.MoveCardDTO;
import com.example.taskmanagerapi.modules.cards.dto.UpdateCardDTO;
import com.example.taskmanagerapi.modules.cards.services.CardService;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;
import com.example.taskmanagerapi.modules.lists.services.BoardListService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * CardController - REST controller for card operations
 */
@RestController
@RequestMapping("/boards/{boardId}/lists/{listId}/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Endpoints for managing cards within lists")
@SecurityRequirement(name = "Bearer Authentication")
public class CardController {
    
    private final CardService cardService;
    private final BoardListService listService;
    private final BoardMemberService boardMemberService;

    @Operation(summary = "Create Card", description = "Create a new card within a list. Any board member can create cards.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Card created successfully",
                content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "List or board not found â€” `LIST_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member â€” `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping
    public ResponseEntity<Object> createCard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @Valid @RequestBody CreateCardDTO body,
            @AuthenticationPrincipal User user) {
        
        Optional<BoardList> listOpt = listService.getListById(listId);
        if (listOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "LIST_NOT_FOUND", "List not found.", 404
            ));
        }
        
        BoardList list = listOpt.get();
        
        if (!list.getBoard().getId().equals(boardId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "LIST_NOT_FOUND", "List does not belong to this board.", 404
            ));
        }
        
        if (!boardMemberService.isMember(list.getBoard(), user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "You don't have permission to add cards to this list.", 403
            ));
        }
        
        CardResponseDTO response = cardService.createCard(body, list);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get All Cards", description = "Retrieve all cards from a list ordered by position")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "List not found â€” `LIST_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member â€” `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    public ResponseEntity<Object> getAllCards(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @AuthenticationPrincipal User user) {
        
        Optional<BoardList> listOpt = listService.getListById(listId);
        if (listOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "LIST_NOT_FOUND", "List not found.", 404
            ));
        }
        
        BoardList list = listOpt.get();
        
        if (!list.getBoard().getId().equals(boardId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "LIST_NOT_FOUND", "List does not belong to this board.", 404
            ));
        }
        
        if (!boardMemberService.isMember(list.getBoard(), user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "You don't have permission to view cards from this list.", 403
            ));
        }
        
        List<CardResponseDTO> response = cardService.getCardsByList(list);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Card by ID", description = "Retrieve a specific card")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card found",
                content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Card not found â€” `CARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member â€” `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{cardId}")
    public ResponseEntity<Object> getCardById(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @AuthenticationPrincipal User user) {
        
        Optional<Card> cardOpt = cardService.getCardById(cardId);
        if (cardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "CARD_NOT_FOUND", "Card not found.", 404
            ));
        }
        
        Card card = cardOpt.get();
        
        if (!card.getList().getId().equals(listId) || !card.getList().getBoard().getId().equals(boardId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "CARD_NOT_FOUND", "Card not found in this list/board.", 404
            ));
        }
        
        if (!boardMemberService.isMember(card.getList().getBoard(), user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "You don't have permission to view this card.", 403
            ));
        }
        
        return ResponseEntity.ok(new CardResponseDTO(card));
    }

    @Operation(summary = "Update Card", description = "Update an existing card's name, description, status or position. Any board member can update cards.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card updated successfully",
                content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Card not found â€” `CARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member â€” `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{cardId}")
    public ResponseEntity<Object> updateCard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @Valid @RequestBody UpdateCardDTO body,
            @AuthenticationPrincipal User user) {
        
        Optional<Card> cardOpt = cardService.getCardById(cardId);
        if (cardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "CARD_NOT_FOUND", "Card not found.", 404
            ));
        }
        
        Card card = cardOpt.get();
        
        if (!card.getList().getId().equals(listId) || !card.getList().getBoard().getId().equals(boardId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "CARD_NOT_FOUND", "Card not found in this list/board.", 404
            ));
        }
        
        if (!boardMemberService.isMember(card.getList().getBoard(), user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "You don't have permission to update this card.", 403
            ));
        }
        
        CardResponseDTO response = cardService.updateCard(card, body);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Move Card", description = "Move a card to a different list (drag-and-drop). Any board member can move cards.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card moved successfully",
                content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Card or target list not found â€” `CARD_NOT_FOUND`, `LIST_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Target list does not belong to this board â€” `INVALID_MOVE`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member â€” `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PatchMapping("/{cardId}/move")
    public ResponseEntity<Object> moveCard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "Current list ID", required = true) @PathVariable @NonNull String listId,
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @Valid @RequestBody MoveCardDTO body,
            @AuthenticationPrincipal User user) {

        Optional<Card> cardOpt = cardService.getCardById(cardId);
        if (cardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "CARD_NOT_FOUND", "Card not found.", 404
            ));
        }

        Card card = cardOpt.get();

        if (!card.getList().getId().equals(listId) || !card.getList().getBoard().getId().equals(boardId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "CARD_NOT_FOUND", "Card not found in this list/board.", 404
            ));
        }

        if (!boardMemberService.isMember(card.getList().getBoard(), user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "You don't have permission to move this card.", 403
            ));
        }

        String targetListId = Objects.requireNonNull(body.targetListId(), "targetListId is required");
        Optional<BoardList> targetListOpt = listService.getListById(targetListId);
        if (targetListOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "LIST_NOT_FOUND", "Target list not found.", 404
            ));
        }

        BoardList targetList = targetListOpt.get();

        if (!targetList.getBoard().getId().equals(boardId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDTO(
                "INVALID_MOVE", "Target list does not belong to this board.", 400
            ));
        }

        CardResponseDTO response = cardService.moveCard(card, targetList, body);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Card", description = "Delete a card by its ID. Only the board owner can delete cards.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Card deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Card not found â€” `CARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not the board owner â€” `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Object> deleteCard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @AuthenticationPrincipal User user) {
        
        Optional<Card> cardOpt = cardService.getCardById(cardId);
        if (cardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "CARD_NOT_FOUND", "Card not found.", 404
            ));
        }
        
        Card card = cardOpt.get();
        
        if (!card.getList().getId().equals(listId) || !card.getList().getBoard().getId().equals(boardId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "CARD_NOT_FOUND", "Card not found in this list/board.", 404
            ));
        }
        
        if (!card.getList().getBoard().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "Only the board owner can delete cards.", 403
            ));
        }
        
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
}
