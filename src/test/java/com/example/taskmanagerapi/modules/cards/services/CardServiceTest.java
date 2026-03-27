package com.example.taskmanagerapi.modules.cards.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import com.example.taskmanagerapi.modules.cards.domain.Card;
import com.example.taskmanagerapi.modules.cards.domain.CardStatus;
import com.example.taskmanagerapi.modules.cards.dto.CardResponseDTO;
import com.example.taskmanagerapi.modules.cards.dto.CreateCardDTO;
import com.example.taskmanagerapi.modules.cards.dto.MoveCardDTO;
import com.example.taskmanagerapi.modules.cards.dto.UpdateCardDTO;
import com.example.taskmanagerapi.modules.cards.repositories.CardRepository;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardService")
class CardServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private AttachmentService attachmentService;

    @InjectMocks
    private CardService cardService;

    private Board testBoard;
    private BoardList testList;
    private BoardList targetList;
    private Card testCard;

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

        targetList = new BoardList();
        targetList.setId("list-2");
        targetList.setName("In Progress");
        targetList.setPosition(1);
        targetList.setBoard(testBoard);

        testCard = new Card();
        testCard.setId("card-1");
        testCard.setName("Test Card");
        testCard.setDescription("A test card");
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setPosition(0);
        testCard.setList(testList);
        testCard.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createCard")
    class CreateCard {

        @Test
        @DisplayName("should create card with default status and auto position")
        void shouldCreateWithDefaults() {
            CreateCardDTO dto = new CreateCardDTO("New Card", "Description", null, null);

            when(cardRepository.findMaxPositionByList(testList)).thenReturn(null);
            when(cardRepository.save(any(Card.class))).thenReturn(testCard);

            CardResponseDTO result = cardService.createCard(dto, testList);

            assertThat(result.id()).isEqualTo("card-1");
            assertThat(result.name()).isEqualTo("Test Card");
        }

        @Test
        @DisplayName("should create card with explicit position")
        void shouldCreateWithExplicitPosition() {
            CreateCardDTO dto = new CreateCardDTO("New Card", "Description", CardStatus.ACTIVE, 5);

            when(cardRepository.save(any(Card.class))).thenReturn(testCard);

            CardResponseDTO result = cardService.createCard(dto, testList);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should auto-append at end when no position specified")
        void shouldAutoAppend() {
            CreateCardDTO dto = new CreateCardDTO("New Card", null, null, null);

            when(cardRepository.findMaxPositionByList(testList)).thenReturn(3);
            when(cardRepository.save(any(Card.class))).thenReturn(testCard);

            cardService.createCard(dto, testList);

            verify(cardRepository).save(any(Card.class));
        }
    }

    @Nested
    @DisplayName("getCardsByList")
    class GetCardsByList {

        @Test
        @DisplayName("should return cards ordered by position")
        void shouldReturnCards() {
            when(cardRepository.findByListOrderByPositionAsc(testList))
                    .thenReturn(List.of(testCard));

            List<CardResponseDTO> result = cardService.getCardsByList(testList);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Test Card");
        }
    }

    @Nested
    @DisplayName("requireCardById")
    class RequireCardById {

        @Test
        @DisplayName("should return card when found")
        void shouldReturnCard() {
            when(cardRepository.findById("card-1")).thenReturn(Optional.of(testCard));

            Card result = cardService.requireCardById("card-1");

            assertThat(result.getId()).isEqualTo("card-1");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(cardRepository.findById("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.requireCardById("unknown"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for null id")
        void shouldThrowForNullId() {
            assertThatThrownBy(() -> cardService.requireCardById(null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for blank id")
        void shouldThrowForBlankId() {
            assertThatThrownBy(() -> cardService.requireCardById(""))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("requireCardByContext")
    class RequireCardByContext {

        @Test
        @DisplayName("should return card when context matches")
        void shouldReturnWhenContextMatches() {
            when(cardRepository.findById("card-1")).thenReturn(Optional.of(testCard));

            Card result = cardService.requireCardByContext("card-1", "list-1", "board-1");

            assertThat(result.getId()).isEqualTo("card-1");
        }

        @Test
        @DisplayName("should throw when list id doesn't match")
        void shouldThrowWhenListMismatch() {
            when(cardRepository.findById("card-1")).thenReturn(Optional.of(testCard));

            assertThatThrownBy(() -> cardService.requireCardByContext("card-1", "wrong-list", "board-1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when board id doesn't match")
        void shouldThrowWhenBoardMismatch() {
            when(cardRepository.findById("card-1")).thenReturn(Optional.of(testCard));

            assertThatThrownBy(() -> cardService.requireCardByContext("card-1", "list-1", "wrong-board"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateCard")
    class UpdateCard {

        @Test
        @DisplayName("should update card fields")
        void shouldUpdateCard() {
            UpdateCardDTO dto = new UpdateCardDTO("Updated Name", "New desc", CardStatus.COMPLETED, 2);

            when(cardRepository.save(any(Card.class))).thenReturn(testCard);

            CardResponseDTO result = cardService.updateCard(testCard, dto);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should ignore null fields")
        void shouldIgnoreNullFields() {
            UpdateCardDTO dto = new UpdateCardDTO(null, null, null, null);

            when(cardRepository.save(any(Card.class))).thenReturn(testCard);

            cardService.updateCard(testCard, dto);

            assertThat(testCard.getName()).isEqualTo("Test Card");
            assertThat(testCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("moveCard")
    class MoveCard {

        @Test
        @DisplayName("should move card to target list with explicit position")
        void shouldMoveWithExplicitPosition() {
            MoveCardDTO dto = new MoveCardDTO("list-2", 0);

            when(cardRepository.save(any(Card.class))).thenReturn(testCard);

            CardResponseDTO result = cardService.moveCard(testCard, targetList, dto);

            assertThat(result).isNotNull();
            assertThat(testCard.getList()).isEqualTo(targetList);
        }

        @Test
        @DisplayName("should move card to end of list when no position specified")
        void shouldMoveToEnd() {
            MoveCardDTO dto = new MoveCardDTO("list-2", null);

            when(cardRepository.findMaxPositionByList(targetList)).thenReturn(5);
            when(cardRepository.save(any(Card.class))).thenReturn(testCard);

            cardService.moveCard(testCard, targetList, dto);

            assertThat(testCard.getPosition()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("deleteCard")
    class DeleteCard {

        @Test
        @DisplayName("should delete card and its attachments")
        void shouldDeleteCard() {
            when(cardRepository.findById("card-1")).thenReturn(Optional.of(testCard));

            cardService.deleteCard("card-1");

            verify(attachmentService).deleteAllByCard(testCard);
            verify(cardRepository).deleteById("card-1");
        }

        @Test
        @DisplayName("should do nothing for null id")
        void shouldDoNothingForNullId() {
            cardService.deleteCard(null);
        }

        @Test
        @DisplayName("should do nothing for blank id")
        void shouldDoNothingForBlankId() {
            cardService.deleteCard("");
        }
    }
}
