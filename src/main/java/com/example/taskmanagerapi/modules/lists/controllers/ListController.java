package com.example.taskmanagerapi.modules.lists.controllers;

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
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.services.BoardService;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;
import com.example.taskmanagerapi.modules.lists.dto.CreateListDTO;
import com.example.taskmanagerapi.modules.lists.dto.ListResponseDTO;
import com.example.taskmanagerapi.modules.lists.dto.UpdateListDTO;
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
 * ListController - REST controller for list operations
 * Single Responsibility: Handle HTTP requests for lists
 * Delegates business logic to BoardListService
 */
@RestController
@RequestMapping("/boards/{boardId}/lists")
@RequiredArgsConstructor
@Tag(name = "Lists", description = "Endpoints for managing lists within boards")
@SecurityRequirement(name = "Bearer Authentication")
public class ListController {
    
    private final BoardListService listService;
    private final BoardService boardService;

    @Operation(summary = "Create List", description = "Create a new list within a board")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "List created successfully",
                content = @Content(schema = @Schema(implementation = ListResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Board not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping
    public ResponseEntity<Object> createList(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @RequestBody CreateListDTO body,
            @AuthenticationPrincipal User user) {
        
        if (body == null) {
            return ResponseEntity.badRequest().body("Request body is required");
        }
        
        // Validate board exists and belongs to user
        Optional<Board> boardOpt = boardService.getBoardById(boardId);
        if (boardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Board not found");
        }
        
        Board board = boardOpt.get();
        
        if (!board.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to add lists to this board");
        }
        
        ListResponseDTO response = listService.createList(body, board);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get All Lists", description = "Retrieve all lists from a board ordered by position")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lists retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Board not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    public ResponseEntity<Object> getAllLists(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @AuthenticationPrincipal User user) {
        
        Optional<Board> boardOpt = boardService.getBoardById(boardId);
        if (boardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Board not found");
        }
        
        Board board = boardOpt.get();
        
        if (!board.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to view lists from this board");
        }
        
        List<ListResponseDTO> response = listService.getListsByBoard(board);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get List by ID", description = "Retrieve a specific list")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List found",
                content = @Content(schema = @Schema(implementation = ListResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "List not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{listId}")
    public ResponseEntity<Object> getListById(
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
                    .body("You don't have permission to view this list");
        }
        
        return ResponseEntity.ok(new ListResponseDTO(list));
    }

    @Operation(summary = "Update List", description = "Update an existing list")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List updated successfully",
                content = @Content(schema = @Schema(implementation = ListResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "List not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{listId}")
    public ResponseEntity<Object> updateList(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @RequestBody UpdateListDTO body,
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
                    .body("You don't have permission to update this list");
        }
        
        if (body == null) {
            return ResponseEntity.badRequest().body("Request body is required");
        }
        
        ListResponseDTO response = listService.updateList(list, body);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete List", description = "Delete a list by its ID (cascades to all cards)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List deleted successfully"),
        @ApiResponse(responseCode = "404", description = "List not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @DeleteMapping("/{listId}")
    public ResponseEntity<Object> deleteList(
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
                    .body("You don't have permission to delete this list");
        }
        
        listService.deleteList(listId);
        return ResponseEntity.ok("List and all associated cards deleted successfully");
    }
}
