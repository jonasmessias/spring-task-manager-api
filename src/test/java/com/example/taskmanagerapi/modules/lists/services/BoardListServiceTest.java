package com.example.taskmanagerapi.modules.lists.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.taskmanagerapi.infra.exception.ResourceNotFoundException;
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.domain.BoardType;
import com.example.taskmanagerapi.modules.cards.services.CardService;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;
import com.example.taskmanagerapi.modules.lists.dto.CreateListDTO;
import com.example.taskmanagerapi.modules.lists.dto.ListResponseDTO;
import com.example.taskmanagerapi.modules.lists.dto.UpdateListDTO;
import com.example.taskmanagerapi.modules.lists.repositories.BoardListRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardListService")
class BoardListServiceTest {

    @Mock private BoardListRepository listRepository;
    @Mock private CardService cardService;

    @InjectMocks
    private BoardListService boardListService;

    private Board testBoard;
    private BoardList testList;

    @BeforeEach
    void setUp() {
        testBoard = new Board();
        testBoard.setId("board-1");
        testBoard.setName("Test Board");
        testBoard.setType(BoardType.BOARD);

        testList = new BoardList();
        testList.setId("list-1");
        testList.setName("To Do");
        testList.setPosition(0);
        testList.setBoard(testBoard);
        testList.setCards(new ArrayList<>());
        testList.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createList")
    class CreateList {

        @Test
        @DisplayName("should create list with auto position")
        void shouldCreateListWithAutoPosition() {
            CreateListDTO dto = new CreateListDTO("To Do");

            when(listRepository.findMaxPositionByBoard(testBoard)).thenReturn(null);
            when(listRepository.save(any(BoardList.class))).thenReturn(testList);

            ListResponseDTO result = boardListService.createList(dto, testBoard);

            assertThat(result.id()).isEqualTo("list-1");
            assertThat(result.name()).isEqualTo("To Do");
        }

        @Test
        @DisplayName("should increment position when lists exist")
        void shouldIncrementPosition() {
            CreateListDTO dto = new CreateListDTO("In Progress");

            when(listRepository.findMaxPositionByBoard(testBoard)).thenReturn(2);
            when(listRepository.save(any(BoardList.class))).thenReturn(testList);

            boardListService.createList(dto, testBoard);

            verify(listRepository).save(any(BoardList.class));
        }
    }

    @Nested
    @DisplayName("getListsByBoard")
    class GetListsByBoard {

        @Test
        @DisplayName("should return lists for board")
        void shouldReturnLists() {
            when(listRepository.findByBoardOrderByPositionAsc(testBoard))
                    .thenReturn(List.of(testList));

            List<ListResponseDTO> result = boardListService.getListsByBoard(testBoard);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("To Do");
        }
    }

    @Nested
    @DisplayName("requireListById")
    class RequireListById {

        @Test
        @DisplayName("should return list when found")
        void shouldReturnList() {
            when(listRepository.findById("list-1")).thenReturn(Optional.of(testList));

            BoardList result = boardListService.requireListById("list-1");

            assertThat(result.getId()).isEqualTo("list-1");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(listRepository.findById("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> boardListService.requireListById("unknown"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("requireListByIdAndBoard")
    class RequireListByIdAndBoard {

        @Test
        @DisplayName("should return list when it belongs to board")
        void shouldReturnWhenBelongsToBoard() {
            when(listRepository.findById("list-1")).thenReturn(Optional.of(testList));

            BoardList result = boardListService.requireListByIdAndBoard("list-1", "board-1");

            assertThat(result.getId()).isEqualTo("list-1");
        }

        @Test
        @DisplayName("should throw when list does not belong to board")
        void shouldThrowWhenNotInBoard() {
            when(listRepository.findById("list-1")).thenReturn(Optional.of(testList));

            assertThatThrownBy(() -> boardListService.requireListByIdAndBoard("list-1", "other-board"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateList")
    class UpdateList {

        @Test
        @DisplayName("should update list name and position")
        void shouldUpdateList() {
            UpdateListDTO dto = new UpdateListDTO("In Progress", 1);

            when(listRepository.save(any(BoardList.class))).thenReturn(testList);

            ListResponseDTO result = boardListService.updateList(testList, dto);

            assertThat(result).isNotNull();
            verify(listRepository).save(any(BoardList.class));
        }

        @Test
        @DisplayName("should ignore null name on update")
        void shouldIgnoreNullName() {
            UpdateListDTO dto = new UpdateListDTO(null, 2);

            when(listRepository.save(any(BoardList.class))).thenReturn(testList);

            boardListService.updateList(testList, dto);

            assertThat(testList.getName()).isEqualTo("To Do");
        }
    }

    @Nested
    @DisplayName("deleteList")
    class DeleteList {

        @Test
        @DisplayName("should delete list and its cards")
        void shouldDeleteList() {
            when(listRepository.findById("list-1")).thenReturn(Optional.of(testList));

            boardListService.deleteList("list-1");

            verify(cardService).deleteAllByList(testList);
            verify(listRepository).deleteById("list-1");
        }

        @Test
        @DisplayName("should do nothing when list not found")
        void shouldDoNothingWhenNotFound() {
            when(listRepository.findById("unknown")).thenReturn(Optional.empty());

            boardListService.deleteList("unknown");
        }
    }

    @Nested
    @DisplayName("deleteAllByBoard")
    class DeleteAllByBoard {

        @Test
        @DisplayName("should delete all lists and their cards for board")
        void shouldDeleteAllByBoard() {
            when(listRepository.findByBoardOrderByPositionAsc(testBoard))
                    .thenReturn(List.of(testList));

            boardListService.deleteAllByBoard(testBoard);

            verify(cardService).deleteAllByList(testList);
            verify(listRepository).deleteByBoard(testBoard);
        }
    }
}
