package com.example.taskmanagerapi.modules.boards.controllers;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.dto.ErrorResponseDTO;
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.dto.BoardDetailDTO;
import com.example.taskmanagerapi.modules.boards.dto.BoardResponseDTO;
import com.example.taskmanagerapi.modules.boards.dto.CreateBoardDTO;
import com.example.taskmanagerapi.modules.boards.dto.UpdateBoardDTO;
import com.example.taskmanagerapi.modules.boards.services.BoardService;
import com.example.taskmanagerapi.modules.storage.services.StorageService;
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
    private final StorageService storageService;

    private static final String BOARD_COVER_FOLDER = "covers/boards";

    @Operation(summary = "Create Board", description = "Create a new board within a workspace. Any workspace member can create boards.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Board created successfully",
                content = @Content(schema = @Schema(implementation = BoardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Workspace not found — `WORKSPACE_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a workspace member — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping
    public ResponseEntity<BoardResponseDTO> createBoard(
            @Valid @RequestBody CreateBoardDTO body,
            @Parameter(description = "Workspace ID", required = true)
            @RequestParam("workspaceId") String workspaceId,
            @AuthenticationPrincipal User user) {
        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        workspaceService.requireMember(workspace, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.createBoard(body, user, workspace));
    }

    @Operation(summary = "Get All Boards", description = "Retrieve all boards for a workspace. Any workspace member can list boards.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Boards retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Workspace not found — `WORKSPACE_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a workspace member — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    public ResponseEntity<?> getAllBoards(
            @Parameter(description = "Workspace ID", required = true)
            @RequestParam("workspaceId") String workspaceId,
            @Parameter(description = "Page number (0-based). Omit for unpaginated results.")
            @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Page size. Default: 20")
            @RequestParam(value = "size", required = false, defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        workspaceService.requireMember(workspace, user);

        if (page != null) {
            Pageable pageable = PageRequest.of(page, size);
            Page<BoardResponseDTO> response = boardService.getBoardsByWorkspace(workspace, pageable);
            return ResponseEntity.ok(response);
        }

        List<BoardResponseDTO> response = boardService.getBoardsByWorkspace(workspace);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Board by ID", description = "Retrieve a specific board with all its lists and cards. Any board member can view.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Board found",
                content = @Content(schema = @Schema(implementation = BoardDetailDTO.class))),
        @ApiResponse(responseCode = "404", description = "Board not found — `BOARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a board member — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BoardDetailDTO> getBoardById(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String id,
            @AuthenticationPrincipal User user) {
        Board board = boardService.requireBoardById(id);
        boardService.requireMember(board, user);
        return ResponseEntity.ok(new BoardDetailDTO(board));
    }

    @Operation(summary = "Update Board", description = "Update an existing board. Only the board owner can update.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Board updated successfully",
                content = @Content(schema = @Schema(implementation = BoardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Board not found — `BOARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not the board owner — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BoardResponseDTO> updateBoard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String id,
            @Valid @RequestBody UpdateBoardDTO body,
            @AuthenticationPrincipal User user) {
        Board board = boardService.requireBoardById(id);
        boardService.requireOwner(board, user);
        return ResponseEntity.ok(boardService.updateBoard(board, body));
    }

    @Operation(summary = "Delete Board", description = "Delete a board by its ID. Only the board owner can delete.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Board deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Board not found — `BOARD_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not the board owner — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String id,
            @AuthenticationPrincipal User user) {
        Board board = boardService.requireBoardById(id);
        boardService.requireOwner(board, user);
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload Board Cover", description = "Upload or replace the board cover image. Only the board owner can upload. Accepts JPG, PNG and WebP up to 5MB.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cover uploaded successfully",
                content = @Content(schema = @Schema(implementation = BoardResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Board not found"),
        @ApiResponse(responseCode = "403", description = "Not the board owner"),
        @ApiResponse(responseCode = "400", description = "Invalid file"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BoardResponseDTO> uploadBoardCover(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {
        Board board = boardService.requireBoardById(id);
        boardService.requireOwner(board, user);

        if (board.getCoverUrl() != null) {
            storageService.deleteFile(board.getCoverUrl());
        }

        String fileUrl = storageService.uploadFile(file, BOARD_COVER_FOLDER);
        board.setCoverUrl(fileUrl);
        Board saved = boardService.saveBoard(board);

        return ResponseEntity.ok(new BoardResponseDTO(saved));
    }

    @Operation(summary = "Delete Board Cover", description = "Remove the board cover image. Only the board owner can delete.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Cover removed successfully"),
        @ApiResponse(responseCode = "404", description = "Board not found"),
        @ApiResponse(responseCode = "403", description = "Not the board owner"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{id}/cover")
    public ResponseEntity<Void> deleteBoardCover(
            @Parameter(description = "Board ID", required = true) @PathVariable @NonNull String id,
            @AuthenticationPrincipal User user) {
        Board board = boardService.requireBoardById(id);
        boardService.requireOwner(board, user);

        if (board.getCoverUrl() != null) {
            storageService.deleteFile(board.getCoverUrl());
            board.setCoverUrl(null);
            boardService.saveBoard(board);
        }

        return ResponseEntity.noContent().build();
    }
}
