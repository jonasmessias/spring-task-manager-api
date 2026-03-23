package com.example.taskmanagerapi.modules.cards.controllers;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanagerapi.infra.exception.BusinessException;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.dto.ErrorResponseDTO;
import com.example.taskmanagerapi.modules.boards.services.BoardService;
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

@RestController
@RequestMapping("/boards/{boardId}/lists/{listId}/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Endpoints for managing cards within lists")
@SecurityRequirement(name = "Bearer Authentication")
public class CardController {
    
    private final CardService cardService;
    private final BoardListService listService;
    private final BoardService boardService;

    @Operation(summary = "Create Card", description = "Create a new card within a list. Any board member can create cards.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Card created successfully",
                content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "List or board not found — `LIST_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping
    public ResponseEntity<CardResponseDTO> createCard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @Valid @RequestBody CreateCardDTO body,
            @AuthenticationPrincipal User user) {
        BoardList list = listService.requireListByIdAndBoard(listId, boardId);
        boardService.requireMember(list.getBoard(), user);
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(body, list));
    }

    @Operation(summary = "Get All Cards", description = "Retrieve all cards from a list ordered by position")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "List not found — `LIST_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    public ResponseEntity<?> getAllCards(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @Parameter(description = "Page number (0-based). Omit for unpaginated results.")
            @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Page size. Default: 50")
            @RequestParam(value = "size", required = false, defaultValue = "50") int size,
            @AuthenticationPrincipal User user) {
        BoardList list = listService.requireListByIdAndBoard(listId, boardId);
        boardService.requireMember(list.getBoard(), user);

        if (page != null) {
            Pageable pageable = PageRequest.of(page, size);
            Page<CardResponseDTO> response = cardService.getCardsByList(list, pageable);
            return ResponseEntity.ok(response);
        }

        List<CardResponseDTO> response = cardService.getCardsByList(list);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Card by ID", description = "Retrieve a specific card")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card found",
                content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Card not found — `CARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponseDTO> getCardById(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @AuthenticationPrincipal User user) {
        Card card = cardService.requireCardByContext(cardId, listId, boardId);
        boardService.requireMember(card.getList().getBoard(), user);
        return ResponseEntity.ok(new CardResponseDTO(card));
    }

    @Operation(summary = "Update Card", description = "Update an existing card's name, description, status or position. Any board member can update cards.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card updated successfully",
                content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Card not found — `CARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{cardId}")
    public ResponseEntity<CardResponseDTO> updateCard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @Valid @RequestBody UpdateCardDTO body,
            @AuthenticationPrincipal User user) {
        Card card = cardService.requireCardByContext(cardId, listId, boardId);
        boardService.requireMember(card.getList().getBoard(), user);
        return ResponseEntity.ok(cardService.updateCard(card, body));
    }

    @Operation(summary = "Move Card", description = "Move a card to a different list (drag-and-drop). Any board member can move cards.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card moved successfully",
                content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Card or target list not found — `CARD_NOT_FOUND`, `LIST_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Target list does not belong to this board — `INVALID_MOVE`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PatchMapping("/{cardId}/move")
    public ResponseEntity<CardResponseDTO> moveCard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "Current list ID", required = true) @PathVariable @NonNull String listId,
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @Valid @RequestBody MoveCardDTO body,
            @AuthenticationPrincipal User user) {
        Card card = cardService.requireCardByContext(cardId, listId, boardId);
        boardService.requireMember(card.getList().getBoard(), user);

        String targetListId = Objects.requireNonNull(body.targetListId(), "targetListId is required");
        BoardList targetList = listService.requireListById(targetListId);

        if (!targetList.getBoard().getId().equals(boardId)) {
            throw new BusinessException("INVALID_MOVE", "Target list does not belong to this board.");
        }

        return ResponseEntity.ok(cardService.moveCard(card, targetList, body));
    }

    @Operation(summary = "Delete Card", description = "Delete a card by its ID. Only the board owner can delete cards.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Card deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Card not found — `CARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not the board owner — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @AuthenticationPrincipal User user) {
        Card card = cardService.requireCardByContext(cardId, listId, boardId);
        boardService.requireOwner(card.getList().getBoard(), user);
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
}
