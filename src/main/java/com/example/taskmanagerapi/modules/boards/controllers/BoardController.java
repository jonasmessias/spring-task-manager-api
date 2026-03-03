package com.example.taskmanagerapi.modules.boards.controllers;

import java.util.List;
import java.util.Objects;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.dto.BoardDetailDTO;
import com.example.taskmanagerapi.modules.boards.dto.BoardResponseDTO;
import com.example.taskmanagerapi.modules.boards.dto.CreateBoardDTO;
import com.example.taskmanagerapi.modules.boards.dto.UpdateBoardDTO;
import com.example.taskmanagerapi.modules.boards.services.BoardService;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;
import com.example.taskmanagerapi.modules.workspaces.services.WorkspaceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
@Tag(name = "Boards", description = "Endpoints for managing boards within workspaces")
@SecurityRequirement(name = "Bearer Authentication")
public class BoardController {
    
    private final BoardService boardService;
    private final WorkspaceService workspaceService;

    @Operation(summary = "Create Board", description = "Create a new board within a workspace")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Board created successfully",
                content = @Content(schema = @Schema(implementation = BoardResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or workspace not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Workspace belongs to another user")
    })
    @PostMapping
    public ResponseEntity<Object> createBoard(
            @RequestBody CreateBoardDTO body,
            @Parameter(description = "Workspace ID", required = true) 
            @RequestParam("workspaceId") String workspaceId,
            @AuthenticationPrincipal User user) {
        
        if (body == null) {
            return ResponseEntity.badRequest().body("Request body is required");
        }
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        
        // Validate and get workspace
        Optional<Workspace> workspaceOpt = workspaceService.getWorkspaceById(
            Objects.requireNonNull(workspaceId, "Workspace ID cannot be null")
        );
        if (workspaceOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Workspace not found");
        }
        
        Workspace workspace = workspaceOpt.get();
        
        // Check if workspace belongs to user
        if (!workspaceService.isWorkspaceOwner(
                Objects.requireNonNull(workspace, "Workspace cannot be null"), 
                Objects.requireNonNull(user, "User cannot be null"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to create boards in this workspace");
        }
        
        BoardResponseDTO response = boardService.createBoard(
            Objects.requireNonNull(body, "Request body cannot be null"), 
            Objects.requireNonNull(user, "User cannot be null"), 
            Objects.requireNonNull(workspace, "Workspace cannot be null")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get All Boards", description = "Retrieve all boards for a workspace")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Boards retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Workspace not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Workspace belongs to another user")
    })
    @GetMapping
    public ResponseEntity<Object> getAllBoards(
            @Parameter(description = "Workspace ID", required = true) 
            @RequestParam("workspaceId") String workspaceId,
            @AuthenticationPrincipal User user) {
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        
        // Validate and get workspace
        Optional<Workspace> workspaceOpt = workspaceService.getWorkspaceById(
            Objects.requireNonNull(workspaceId, "Workspace ID cannot be null")
        );
        if (workspaceOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Workspace not found");
        }
        
        Workspace workspace = workspaceOpt.get();
        
        // Check if workspace belongs to user
        if (!workspaceService.isWorkspaceOwner(
                Objects.requireNonNull(workspace, "Workspace cannot be null"), 
                Objects.requireNonNull(user, "User cannot be null"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to view boards in this workspace");
        }
        
        List<BoardResponseDTO> response = boardService.getBoardsByWorkspace(
            Objects.requireNonNull(workspace, "Workspace cannot be null")
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Board by ID", description = "Retrieve a specific board with all its lists and cards")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Board found",
                content = @Content(schema = @Schema(implementation = BoardDetailDTO.class))),
        @ApiResponse(responseCode = "404", description = "Board not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Object> getBoardById(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String id,
            @AuthenticationPrincipal User user) {
        
        Optional<Board> boardOpt = boardService.getBoardById(id);
        
        if (boardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Board not found");
        }
        
        Board board = boardOpt.get();
        
        // Check if board belongs to the authenticated user
        if (!board.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to access this board");
        }
        
        return ResponseEntity.ok(new BoardDetailDTO(board));
    }

    @Operation(summary = "Update Board", description = "Update an existing board")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Board updated successfully",
                content = @Content(schema = @Schema(implementation = BoardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Board not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateBoard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String id,
            @RequestBody UpdateBoardDTO body,
            @AuthenticationPrincipal User user) {
        
        Optional<Board> boardOpt = boardService.getBoardById(id);
        
        if (boardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Board not found");
        }
        
        Board board = boardOpt.get();
        
        // Check if board belongs to the authenticated user
        if (!board.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to update this board");
        }
        
        if (body == null) {
            return ResponseEntity.badRequest().body("Request body is required");
        }
        
        BoardResponseDTO response = boardService.updateBoard(board, body);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Board", description = "Delete a board by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Board deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Board not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Board belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteBoard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String id,
            @AuthenticationPrincipal User user) {
        
        Optional<Board> boardOpt = boardService.getBoardById(id);
        
        if (boardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Board not found");
        }
        
        Board board = boardOpt.get();
        
        // Check if board belongs to the authenticated user
        if (!board.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to delete this board");
        }
        
        boardService.deleteBoard(id);
        return ResponseEntity.ok("Board and all associated lists and cards deleted successfully");
    }
}
