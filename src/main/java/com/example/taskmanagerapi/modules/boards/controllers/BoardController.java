package com.example.taskmanagerapi.modules.boards.controllers;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.dto.ErrorResponseDTO;
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.dto.BoardDetailDTO;
import com.example.taskmanagerapi.modules.boards.dto.BoardResponseDTO;
import com.example.taskmanagerapi.modules.boards.dto.CreateBoardDTO;
import com.example.taskmanagerapi.modules.boards.dto.UpdateBoardDTO;
import com.example.taskmanagerapi.modules.boards.services.BoardMemberService;
import com.example.taskmanagerapi.modules.boards.services.BoardService;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;
import com.example.taskmanagerapi.modules.workspaces.services.WorkspaceMemberService;
import com.example.taskmanagerapi.modules.workspaces.services.WorkspaceService;

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
@RequestMapping("/boards")
@RequiredArgsConstructor
@Tag(name = "Boards", description = "Endpoints for managing boards within workspaces")
@SecurityRequirement(name = "Bearer Authentication")
public class BoardController {
    
    private final BoardService boardService;
    private final WorkspaceService workspaceService;
    private final WorkspaceMemberService workspaceMemberService;
    private final BoardMemberService boardMemberService;

    @Operation(summary = "Create Board", description = "Create a new board within a workspace. Any workspace member can create boards.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Board created successfully",
                content = @Content(schema = @Schema(implementation = BoardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Workspace not found â€” `WORKSPACE_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a workspace member â€” `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping
    public ResponseEntity<Object> createBoard(
            @Valid @RequestBody CreateBoardDTO body,
            @Parameter(description = "Workspace ID", required = true)
            @RequestParam("workspaceId") String workspaceId,
            @AuthenticationPrincipal User user) {
        
        Optional<Workspace> workspaceOpt = workspaceService.getWorkspaceById(workspaceId);
        if (workspaceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "WORKSPACE_NOT_FOUND", "Workspace not found.", 404
            ));
        }
        
        Workspace workspace = workspaceOpt.get();
        
        // Any workspace member can create boards
        if (!workspaceMemberService.isMember(workspace, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "You don't have permission to create boards in this workspace.", 403
            ));
        }
        
        BoardResponseDTO response = boardService.createBoard(body, user, workspace);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get All Boards", description = "Retrieve all boards for a workspace. Any workspace member can list boards.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Boards retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Workspace not found â€” `WORKSPACE_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a workspace member â€” `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    public ResponseEntity<Object> getAllBoards(
            @Parameter(description = "Workspace ID", required = true)
            @RequestParam("workspaceId") String workspaceId,
            @AuthenticationPrincipal User user) {
        
        Optional<Workspace> workspaceOpt = workspaceService.getWorkspaceById(workspaceId);
        if (workspaceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "WORKSPACE_NOT_FOUND", "Workspace not found.", 404
            ));
        }
        
        Workspace workspace = workspaceOpt.get();
        
        // Any workspace member can list boards
        if (!workspaceMemberService.isMember(workspace, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "You don't have permission to view boards in this workspace.", 403
            ));
        }
        
        List<BoardResponseDTO> response = boardService.getBoardsByWorkspace(workspace);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Board by ID", description = "Retrieve a specific board with all its lists and cards. Any board member can view.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Board found",
                content = @Content(schema = @Schema(implementation = BoardDetailDTO.class))),
        @ApiResponse(responseCode = "404", description = "Board not found â€” `BOARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member â€” `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Object> getBoardById(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String id,
            @AuthenticationPrincipal User user) {
        
        Optional<Board> boardOpt = boardService.getBoardById(id);
        
        if (boardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "BOARD_NOT_FOUND", "Board not found.", 404
            ));
        }
        
        Board board = boardOpt.get();
        
        if (!boardMemberService.isMember(board, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "You don't have permission to access this board.", 403
            ));
        }
        
        return ResponseEntity.ok(new BoardDetailDTO(board));
    }

    @Operation(summary = "Update Board", description = "Update an existing board. Only the board owner can update.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Board updated successfully",
                content = @Content(schema = @Schema(implementation = BoardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Board not found â€” `BOARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not the board owner â€” `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateBoard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String id,
            @Valid @RequestBody UpdateBoardDTO body,
            @AuthenticationPrincipal User user) {
        
        Optional<Board> boardOpt = boardService.getBoardById(id);
        
        if (boardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "BOARD_NOT_FOUND", "Board not found.", 404
            ));
        }
        
        Board board = boardOpt.get();
        
        if (!board.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "Only the board owner can update it.", 403
            ));
        }
        
        BoardResponseDTO response = boardService.updateBoard(board, body);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Board", description = "Delete a board by its ID. Only the board owner can delete.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Board deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Board not found â€” `BOARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not the board owner â€” `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteBoard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String id,
            @AuthenticationPrincipal User user) {
        
        Optional<Board> boardOpt = boardService.getBoardById(id);
        
        if (boardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "BOARD_NOT_FOUND", "Board not found.", 404
            ));
        }
        
        Board board = boardOpt.get();
        
        if (!board.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "Only the board owner can delete it.", 403
            ));
        }
        
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }
}
