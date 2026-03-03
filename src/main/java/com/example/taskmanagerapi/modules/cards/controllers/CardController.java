package com.example.taskmanagerapi.modules.cards.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.cards.domain.Card;
import com.example.taskmanagerapi.modules.cards.dto.CardResponseDTO;
import com.example.taskmanagerapi.modules.cards.dto.CreateCardDTO;
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
import lombok.RequiredArgsConstructor;

/**
 * CardController - REST controller for card operations
 * Single Responsibility: Handle HTTP requests for cards
 * Delegates business logic to CardService
 */
@RestController
@RequestMapping("/boards/{boardId}/lists/{listId}/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Endpoints for managing cards within lists")
@SecurityRequirement(name = "Bearer Authentication")
public class CardController {
    
    private final CardService cardService;
    private final BoardListService listService;

    @Operation(summary = "Create Card", description = "Create a new card within a list")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Card created successfully",
                content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "List not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping
    public ResponseEntity<Object> createCard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @RequestBody CreateCardDTO body,
            @AuthenticationPrincipal User user) {
        
        // Validate list exists and belongs to board and user
        Optional<BoardList> listOpt = listService.getListById(listId);
        if (listOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("List not found");
        }
        
        BoardList list = listOpt.get();
        
        if (!list.getBoard().getId().equals(boardId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("List does not belong to this board");
        }
        
        if (!list.getBoard().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to add cards to this list");
        }
        
        CardResponseDTO response = cardService.createCard(body, list);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get All Cards", description = "Retrieve all cards from a list ordered by position")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "List not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    public ResponseEntity<Object> getAllCards(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @AuthenticationPrincipal User user) {
        
        Optional<BoardList> listOpt = listService.getListById(listId);
        if (listOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("List not found");
        }
        
        BoardList list = listOpt.get();
        
        if (!list.getBoard().getId().equals(boardId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("List does not belong to this board");
        }
        
        if (!list.getBoard().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to view cards from this list");
        }
        
        List<CardResponseDTO> response = cardService.getCardsByList(list);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Card by ID", description = "Retrieve a specific card")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card found",
                content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Card not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Card not found");
        }
        
        Card card = cardOpt.get();
        
        if (!card.getList().getId().equals(listId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Card does not belong to this list");
        }
        
        if (!card.getList().getBoard().getId().equals(boardId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("List does not belong to this board");
        }
        
        if (!card.getList().getBoard().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to view this card");
        }
        
        return ResponseEntity.ok(new CardResponseDTO(card));
    }

    @Operation(summary = "Update Card", description = "Update an existing card")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card updated successfully",
                content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Card not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{cardId}")
    public ResponseEntity<Object> updateCard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @RequestBody UpdateCardDTO body,
            @AuthenticationPrincipal User user) {
        
        Optional<Card> cardOpt = cardService.getCardById(cardId);
        if (cardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Card not found");
        }
        
        Card card = cardOpt.get();
        
        if (!card.getList().getId().equals(listId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Card does not belong to this list");
        }
        
        if (!card.getList().getBoard().getId().equals(boardId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("List does not belong to this board");
        }
        
        if (!card.getList().getBoard().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to update this card");
        }
        
        CardResponseDTO response = cardService.updateCard(card, body);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Card", description = "Delete a card by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Card not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Card not found");
        }
        
        Card card = cardOpt.get();
        
        if (!card.getList().getId().equals(listId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Card does not belong to this list");
        }
        
        if (!card.getList().getBoard().getId().equals(boardId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("List does not belong to this board");
        }
        
        if (!card.getList().getBoard().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to delete this card");
        }
        
        cardService.deleteCard(cardId);
        return ResponseEntity.ok("Card deleted successfully");
    }
}
