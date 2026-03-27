package com.example.taskmanagerapi.modules.boards.services;

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

import com.example.taskmanagerapi.infra.exception.ForbiddenException;
import com.example.taskmanagerapi.infra.exception.ResourceNotFoundException;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.domain.BoardType;
import com.example.taskmanagerapi.modules.boards.dto.BoardResponseDTO;
import com.example.taskmanagerapi.modules.boards.dto.CreateBoardDTO;
import com.example.taskmanagerapi.modules.boards.dto.UpdateBoardDTO;
import com.example.taskmanagerapi.modules.boards.repositories.BoardRepository;
import com.example.taskmanagerapi.modules.lists.services.BoardListService;
import com.example.taskmanagerapi.modules.storage.services.StorageService;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoardService")
class BoardServiceTest {

    @Mock private BoardRepository boardRepository;
    @Mock private BoardListService listService;
    @Mock private BoardMemberService boardMemberService;
    @Mock private StorageService storageService;

    @InjectMocks
    private BoardService boardService;

    private User owner;
    private Workspace workspace;
    private Board testBoard;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId("user-1");
        owner.setName("John Doe");

        workspace = new Workspace();
        workspace.setId("ws-1");
        workspace.setName("My Workspace");
        workspace.setOwner(owner);

        testBoard = new Board();
        testBoard.setId("board-1");
        testBoard.setName("Sprint Board");
        testBoard.setType(BoardType.BOARD);
        testBoard.setDescription("Sprint 1 tasks");
        testBoard.setOwner(owner);
        testBoard.setWorkspace(workspace);
        testBoard.setLists(new ArrayList<>());
        testBoard.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createBoard")
    class CreateBoard {

        @Test
        @DisplayName("should create board with default type BOARD")
        void shouldCreateBoardWithDefaultType() {
            CreateBoardDTO dto = new CreateBoardDTO("Sprint Board", null, "Sprint 1 tasks");

            when(boardRepository.save(any(Board.class))).thenReturn(testBoard);

            BoardResponseDTO result = boardService.createBoard(dto, owner, workspace);

            assertThat(result.id()).isEqualTo("board-1");
            assertThat(result.name()).isEqualTo("Sprint Board");
            verify(boardMemberService).addOwner(testBoard, owner);
        }

        @Test
        @DisplayName("should create board with explicit type")
        void shouldCreateBoardWithExplicitType() {
            CreateBoardDTO dto = new CreateBoardDTO("Kanban Board", BoardType.BOARD, "Kanban description");

            when(boardRepository.save(any(Board.class))).thenReturn(testBoard);

            BoardResponseDTO result = boardService.createBoard(dto, owner, workspace);

            assertThat(result).isNotNull();
            verify(boardRepository).save(any(Board.class));
        }
    }

    @Nested
    @DisplayName("getBoardsByWorkspace")
    class GetBoardsByWorkspace {

        @Test
        @DisplayName("should return boards for workspace")
        void shouldReturnBoards() {
            when(boardRepository.findByWorkspaceOrderByCreatedAtDesc(workspace))
                    .thenReturn(List.of(testBoard));

            List<BoardResponseDTO> result = boardService.getBoardsByWorkspace(workspace);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo("board-1");
        }

        @Test
        @DisplayName("should return empty list when no boards")
        void shouldReturnEmptyList() {
            when(boardRepository.findByWorkspaceOrderByCreatedAtDesc(workspace))
                    .thenReturn(List.of());

            List<BoardResponseDTO> result = boardService.getBoardsByWorkspace(workspace);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("requireBoardById")
    class RequireBoardById {

        @Test
        @DisplayName("should return board when found")
        void shouldReturnBoard() {
            when(boardRepository.findById("board-1")).thenReturn(Optional.of(testBoard));

            Board result = boardService.requireBoardById("board-1");

            assertThat(result.getId()).isEqualTo("board-1");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(boardRepository.findById("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> boardService.requireBoardById("unknown"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("requireMember")
    class RequireMember {

        @Test
        @DisplayName("should pass when user is member")
        void shouldPassWhenMember() {
            when(boardMemberService.isMember(testBoard, owner)).thenReturn(true);

            boardService.requireMember(testBoard, owner);
        }

        @Test
        @DisplayName("should throw ForbiddenException when not member")
        void shouldThrowWhenNotMember() {
            when(boardMemberService.isMember(testBoard, owner)).thenReturn(false);

            assertThatThrownBy(() -> boardService.requireMember(testBoard, owner))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("requireOwner")
    class RequireOwner {

        @Test
        @DisplayName("should pass when user is owner")
        void shouldPassWhenOwner() {
            boardService.requireOwner(testBoard, owner);
        }

        @Test
        @DisplayName("should throw ForbiddenException when not owner")
        void shouldThrowWhenNotOwner() {
            User other = new User();
            other.setId("user-other");

            assertThatThrownBy(() -> boardService.requireOwner(testBoard, other))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("updateBoard")
    class UpdateBoard {

        @Test
        @DisplayName("should update board fields")
        void shouldUpdateBoard() {
            UpdateBoardDTO dto = new UpdateBoardDTO("New Name", BoardType.BOARD, "New description");

            when(boardRepository.save(any(Board.class))).thenReturn(testBoard);

            BoardResponseDTO result = boardService.updateBoard(testBoard, dto);

            assertThat(result).isNotNull();
            verify(boardRepository).save(any(Board.class));
        }

        @Test
        @DisplayName("should ignore null fields on update")
        void shouldIgnoreNullFields() {
            UpdateBoardDTO dto = new UpdateBoardDTO(null, null, null);

            when(boardRepository.save(any(Board.class))).thenReturn(testBoard);

            boardService.updateBoard(testBoard, dto);

            assertThat(testBoard.getName()).isEqualTo("Sprint Board");
        }
    }

    @Nested
    @DisplayName("deleteBoard")
    class DeleteBoard {

        @Test
        @DisplayName("should delete board and clean up resources")
        void shouldDeleteBoard() {
            when(boardRepository.findById("board-1")).thenReturn(Optional.of(testBoard));

            boardService.deleteBoard("board-1");

            verify(listService).deleteAllByBoard(testBoard);
            verify(boardRepository).deleteById("board-1");
        }

        @Test
        @DisplayName("should delete board cover from S3")
        void shouldDeleteBoardCover() {
            testBoard.setCoverUrl("https://s3.amazonaws.com/covers/board-1.jpg");
            when(boardRepository.findById("board-1")).thenReturn(Optional.of(testBoard));

            boardService.deleteBoard("board-1");

            verify(storageService).deleteFile("https://s3.amazonaws.com/covers/board-1.jpg");
        }

        @Test
        @DisplayName("should do nothing when board not found")
        void shouldDoNothingWhenNotFound() {
            when(boardRepository.findById("unknown")).thenReturn(Optional.empty());

            boardService.deleteBoard("unknown");
        }
    }

    @Nested
    @DisplayName("isBoardInWorkspace")
    class IsBoardInWorkspace {

        @Test
        @DisplayName("should return true when board belongs to workspace")
        void shouldReturnTrue() {
            assertThat(boardService.isBoardInWorkspace(testBoard, workspace)).isTrue();
        }

        @Test
        @DisplayName("should return false when board does not belong to workspace")
        void shouldReturnFalse() {
            Workspace other = new Workspace();
            other.setId("ws-other");

            assertThat(boardService.isBoardInWorkspace(testBoard, other)).isFalse();
        }
    }
}
