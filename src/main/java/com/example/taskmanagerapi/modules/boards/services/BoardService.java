package com.example.taskmanagerapi.modules.boards.services;

import java.util.List;
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

import lombok.RequiredArgsConstructor;

/**
 * BoardService - Business logic for board operations
 * Single Responsibility: Handle business rules for boards
 * Communicates with BoardListService for cascade operations
 */
@Service
@RequiredArgsConstructor
public class BoardService {
    
    private final BoardRepository boardRepository;
    private final BoardListService listService;

    /**
     * Create a new board for a user
     */
    @Transactional
    public BoardResponseDTO createBoard(@NonNull CreateBoardDTO dto, @NonNull User owner) {
        Board board = new Board();
        board.setName(dto.name());
        board.setType(dto.type() != null ? dto.type() : BoardType.BOARD);
        board.setDescription(dto.description());
        board.setOwner(owner);
        
        Board savedBoard = boardRepository.save(board);
        return new BoardResponseDTO(savedBoard);
    }

    /**
     * Get all boards for a user, ordered by creation date
     */
    public List<BoardResponseDTO> getBoardsByUser(@NonNull User user) {
        return boardRepository.findByOwnerOrderByCreatedAtDesc(user)
                .stream()
                .map(BoardResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get boards by user and type
     */
    public List<BoardResponseDTO> getBoardsByUserAndType(@NonNull User user, @NonNull BoardType type) {
        return boardRepository.findByOwnerAndType(user, type)
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
        
        Board updatedBoard = boardRepository.save(board);
        return new BoardResponseDTO(updatedBoard);
    }

    /**
     * Delete a board by ID
     * Cascades deletion to all lists and cards
     */
    @Transactional
    public void deleteBoard(@NonNull String id) {
        Optional<Board> boardOpt = boardRepository.findById(id);
        if (boardOpt.isPresent()) {
            Board board = boardOpt.get();
            // Delete all lists in the board (which will cascade to cards)
            if (board != null) {
                listService.deleteAllByBoard(board);
            }
            // Then delete the board
            boardRepository.deleteById(id);
        }
    }
}
