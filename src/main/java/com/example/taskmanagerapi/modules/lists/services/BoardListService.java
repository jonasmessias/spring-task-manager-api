package com.example.taskmanagerapi.modules.lists.services;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.cards.services.CardService;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;
import com.example.taskmanagerapi.modules.lists.dto.CreateListDTO;
import com.example.taskmanagerapi.modules.lists.dto.ListResponseDTO;
import com.example.taskmanagerapi.modules.lists.dto.UpdateListDTO;
import com.example.taskmanagerapi.modules.lists.repositories.BoardListRepository;

import lombok.RequiredArgsConstructor;

/**
 * BoardListService - Business logic for list operations
 * Single Responsibility: Handle business rules for lists
 * Communicates with CardService for cascade operations
 */
@Service
@RequiredArgsConstructor
public class BoardListService {
    
    private final BoardListRepository listRepository;
    private final CardService cardService;

    /**
     * Create a new list within a board
     * Auto-sets position based on existing lists
     */
    @Transactional
    public ListResponseDTO createList(@NonNull CreateListDTO dto, @NonNull Board board) {
        // Find the highest position in existing lists
        Integer maxPosition = listRepository.findMaxPositionByBoard(board);
        int newPosition = (maxPosition == null) ? 0 : maxPosition + 1;
        
        BoardList list = new BoardList();
        list.setName(dto.name());
        list.setBoard(board);
        list.setPosition(newPosition);
        
        BoardList savedList = listRepository.save(list);
        return new ListResponseDTO(savedList);
    }

    /**
     * Get all lists from a board ordered by position
     */
    public List<ListResponseDTO> getListsByBoard(@NonNull Board board) {
        return listRepository.findByBoardOrderByPositionAsc(board)
                .stream()
                .map(ListResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Find a list by ID
     */
    public Optional<BoardList> getListById(@NonNull String id) {
        return listRepository.findById(id);
    }

    /**
     * Update an existing list
     * Allows updating name and position
     */
    @Transactional
    public ListResponseDTO updateList(@NonNull BoardList list, @NonNull UpdateListDTO dto) {
        if (dto.name() != null && !dto.name().isBlank()) {
            list.setName(dto.name());
        }
        
        if (dto.position() != null && dto.position() >= 0) {
            list.setPosition(dto.position());
        }
        
        BoardList updatedList = listRepository.save(list);
        return new ListResponseDTO(updatedList);
    }

    /**
     * Delete a list by ID
     * Cascades deletion to all cards in the list
     */
    @Transactional
    public void deleteList(@NonNull String id) {
        Optional<BoardList> listOpt = listRepository.findById(id);
        if (listOpt.isPresent()) {
            BoardList list = listOpt.get();
            // Delete all cards in the list first (cascade)
            cardService.deleteAllByList(list);
            // Then delete the list
            listRepository.deleteById(id);
        }
    }

    /**
     * Delete all lists from a board
     * Used when deleting a board (cascade operation)
     */
    @Transactional
    public void deleteAllByBoard(@NonNull Board board) {
        List<BoardList> lists = listRepository.findByBoardOrderByPositionAsc(board);
        for (BoardList list : lists) {
            // Delete all cards in each list
            cardService.deleteAllByList(list);
        }
        // Delete all lists
        listRepository.deleteByBoard(board);
    }
}
