package com.example.taskmanagerapi.modules.lists.services;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.infra.exception.ResourceNotFoundException;
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.cards.services.CardService;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;
import com.example.taskmanagerapi.modules.lists.dto.CreateListDTO;
import com.example.taskmanagerapi.modules.lists.dto.ListResponseDTO;
import com.example.taskmanagerapi.modules.lists.dto.UpdateListDTO;
import com.example.taskmanagerapi.modules.lists.repositories.BoardListRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardListService {
    
    private final BoardListRepository listRepository;
    private final CardService cardService;

    @Transactional
    public ListResponseDTO createList(@NonNull CreateListDTO dto, @NonNull Board board) {
        Integer maxPosition = listRepository.findMaxPositionByBoard(board);
        int newPosition = (maxPosition == null) ? 0 : maxPosition + 1;
        
        BoardList list = new BoardList();
        list.setName(dto.name());
        list.setBoard(board);
        list.setPosition(newPosition);
        
        BoardList savedList = listRepository.save(list);
        return new ListResponseDTO(savedList);
    }

    public List<ListResponseDTO> getListsByBoard(@NonNull Board board) {
        return listRepository.findByBoardOrderByPositionAsc(board)
                .stream()
                .map(ListResponseDTO::new)
                .collect(Collectors.toList());
    }

    public Optional<BoardList> getListById(@NonNull String id) {
        return listRepository.findById(id);
    }

    public BoardList requireListById(@NonNull String id) {
        return listRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LIST_NOT_FOUND",
                        "List not found."));
    }

    public BoardList requireListByIdAndBoard(@NonNull String listId, @NonNull String boardId) {
        BoardList list = requireListById(listId);
        if (!list.getBoard().getId().equals(boardId)) {
            throw new ResourceNotFoundException("LIST_NOT_FOUND",
                    "List does not belong to this board.");
        }
        return list;
    }

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

    @Transactional
    public void deleteList(@NonNull String id) {
        Optional<BoardList> listOpt = listRepository.findById(id);
        if (listOpt.isPresent()) {
            BoardList list = listOpt.get();
            cardService.deleteAllByList(list);
            listRepository.deleteById(id);
        }
    }

    @Transactional
    public void deleteAllByBoard(@NonNull Board board) {
        List<BoardList> lists = listRepository.findByBoardOrderByPositionAsc(board);
        for (BoardList list : lists) {
            cardService.deleteAllByList(list);
        }
        listRepository.deleteByBoard(board);
    }
}
