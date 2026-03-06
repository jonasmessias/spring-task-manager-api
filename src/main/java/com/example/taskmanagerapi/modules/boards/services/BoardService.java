package com.example.taskmanagerapi.modules.boards.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.domain.BoardType;
import com.example.taskmanagerapi.modules.boards.dto.BoardResponseDTO;
import com.example.taskmanagerapi.modules.boards.dto.CreateBoardDTO;
import com.example.taskmanagerapi.modules.boards.dto.UpdateBoardDTO;
import com.example.taskmanagerapi.modules.boards.repositories.BoardRepository;
import com.example.taskmanagerapi.modules.lists.services.BoardListService;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;

import lombok.RequiredArgsConstructor;

/**
 * BoardService - Business logic for board operations
 */
@Service
@RequiredArgsConstructor
public class BoardService {
    
    private final BoardRepository boardRepository;
    private final BoardListService listService;

    /**
     * Create a new board for a user within a workspace
     */
    @Transactional
    public BoardResponseDTO createBoard(@NonNull CreateBoardDTO dto, @NonNull User owner, @NonNull Workspace workspace) {
        Board board = new Board();
        board.setName(dto.name());
        board.setType(dto.type() != null ? dto.type() : BoardType.BOARD);
        board.setDescription(dto.description());
        board.setOwner(owner);
        board.setWorkspace(workspace);
        
        Board savedBoard = boardRepository.save(board);
        return new BoardResponseDTO(savedBoard);
    }

    /**
     * Get all boards for a workspace, ordered by creation date
     */
    public List<BoardResponseDTO> getBoardsByWorkspace(@NonNull Workspace workspace) {
        return boardRepository.findByWorkspaceOrderByCreatedAtDesc(workspace)
                .stream()
                .map(BoardResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get boards by workspace and type
     */
    public List<BoardResponseDTO> getBoardsByWorkspaceAndType(@NonNull Workspace workspace, @NonNull BoardType type) {
        return boardRepository.findByWorkspaceAndType(workspace, type)
                .stream()
                .map(BoardResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Find a board by ID
     */
    public Optional<Board> getBoardById(@NonNull String id) {
        return boardRepository.findById(id);
    }

    /**
     * Update an existing board
     */
    @Transactional
    public BoardResponseDTO updateBoard(@NonNull Board board, @NonNull UpdateBoardDTO dto) {
        if (dto.name() != null && !dto.name().isBlank()) {
            board.setName(dto.name());
        }
        if (dto.type() != null) {
            board.setType(dto.type());
        }
        if (dto.description() != null) {
            board.setDescription(dto.description());
        }
        board.setUpdatedAt(LocalDateTime.now());
        Board updatedBoard = boardRepository.save(board);
        return new BoardResponseDTO(updatedBoard);
    }

    /**
     * Delete a board by ID â€” cascades to all lists and cards
     */
    @Transactional
    public void deleteBoard(@NonNull String id) {
        Optional<Board> boardOpt = boardRepository.findById(id);
        if (boardOpt.isPresent()) {
            Board board = Objects.requireNonNull(boardOpt.get());
            listService.deleteAllByBoard(board);
            boardRepository.deleteById(id);
        }
    }

    /**
     * Check if a board belongs to a workspace
     */
    public boolean isBoardInWorkspace(@NonNull Board board, @NonNull Workspace workspace) {
        return board.getWorkspace().getId().equals(workspace.getId());
    }

    /**
     * Count boards in a workspace
     */
    public long countWorkspaceBoards(@NonNull Workspace workspace) {
        return boardRepository.countByWorkspace(workspace);
    }
}
