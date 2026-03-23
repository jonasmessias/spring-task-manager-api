package com.example.taskmanagerapi.modules.lists.controllers;

import java.util.List;

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
import com.example.taskmanagerapi.modules.auth.dto.ErrorResponseDTO;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/boards/{boardId}/lists")
@RequiredArgsConstructor
@Tag(name = "Lists", description = "Endpoints for managing lists within boards")
@SecurityRequirement(name = "Bearer Authentication")
public class ListController {
    
    private final BoardListService listService;
    private final BoardService boardService;

    @Operation(summary = "Create List", description = "Create a new list within a board. Any board member can create lists.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "List created successfully",
                content = @Content(schema = @Schema(implementation = ListResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Board not found — `BOARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping
    public ResponseEntity<ListResponseDTO> createList(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Valid @RequestBody CreateListDTO body,
            @AuthenticationPrincipal User user) {
        Board board = boardService.requireBoardById(boardId);
        boardService.requireMember(board, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(listService.createList(body, board));
    }

    @Operation(summary = "Get All Lists", description = "Retrieve all lists from a board ordered by position")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lists retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Board not found — `BOARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    public ResponseEntity<List<ListResponseDTO>> getAllLists(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @AuthenticationPrincipal User user) {
        Board board = boardService.requireBoardById(boardId);
        boardService.requireMember(board, user);
        return ResponseEntity.ok(listService.getListsByBoard(board));
    }

    @Operation(summary = "Get List by ID", description = "Retrieve a specific list")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List found",
                content = @Content(schema = @Schema(implementation = ListResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "List or board not found — `LIST_NOT_FOUND`, `BOARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{listId}")
    public ResponseEntity<ListResponseDTO> getListById(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @AuthenticationPrincipal User user) {
        BoardList list = listService.requireListByIdAndBoard(listId, boardId);
        boardService.requireMember(list.getBoard(), user);
        return ResponseEntity.ok(new ListResponseDTO(list));
    }

    @Operation(summary = "Update List", description = "Update an existing list. Any board member can update lists.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List updated successfully",
                content = @Content(schema = @Schema(implementation = ListResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "List not found — `LIST_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{listId}")
    public ResponseEntity<ListResponseDTO> updateList(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @Valid @RequestBody UpdateListDTO body,
            @AuthenticationPrincipal User user) {
        BoardList list = listService.requireListByIdAndBoard(listId, boardId);
        boardService.requireMember(list.getBoard(), user);
        return ResponseEntity.ok(listService.updateList(list, body));
    }

    @Operation(summary = "Delete List", description = "Delete a list by its ID (cascades to all cards). Only the board owner can delete lists.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "List deleted successfully"),
        @ApiResponse(responseCode = "404", description = "List not found — `LIST_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not the board owner — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String boardId,
            @Parameter(description = "List ID", required = true) @PathVariable @NonNull String listId,
            @AuthenticationPrincipal User user) {
        BoardList list = listService.requireListByIdAndBoard(listId, boardId);
        boardService.requireOwner(list.getBoard(), user);
        listService.deleteList(listId);
        return ResponseEntity.noContent().build();
    }
}
