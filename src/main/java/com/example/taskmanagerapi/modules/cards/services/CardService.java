package com.example.taskmanagerapi.modules.cards.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.infra.exception.ResourceNotFoundException;
import com.example.taskmanagerapi.modules.cards.domain.Card;
import com.example.taskmanagerapi.modules.cards.domain.CardStatus;
import com.example.taskmanagerapi.modules.cards.dto.CardResponseDTO;
import com.example.taskmanagerapi.modules.cards.dto.CreateCardDTO;
import com.example.taskmanagerapi.modules.cards.dto.MoveCardDTO;
import com.example.taskmanagerapi.modules.cards.dto.UpdateCardDTO;
import com.example.taskmanagerapi.modules.cards.repositories.CardRepository;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardService {
    
    private final CardRepository cardRepository;
    private final AttachmentService attachmentService;

    @Transactional
    public CardResponseDTO createCard(CreateCardDTO dto, BoardList list) {
        Card card = new Card();
        card.setName(dto.name());
        card.setDescription(dto.description());
        card.setStatus(dto.status() != null ? dto.status() : CardStatus.ACTIVE);
        card.setList(list);
        card.setCreatedAt(LocalDateTime.now());

        if (dto.position() != null) {
            card.setPosition(dto.position());
        } else {
            Integer maxPosition = cardRepository.findMaxPositionByList(list);
            card.setPosition(maxPosition == null ? 0 : maxPosition + 1);
        }
        
        Card savedCard = cardRepository.save(card);
        return new CardResponseDTO(savedCard);
    }

    public List<CardResponseDTO> getCardsByList(BoardList list) {
        return cardRepository.findByListOrderByPositionAsc(list)
                .stream()
                .map(CardResponseDTO::new)
                .toList();
    }

    public Page<CardResponseDTO> getCardsByList(BoardList list, Pageable pageable) {
        return cardRepository.findByListOrderByPositionAsc(list, pageable)
                .map(CardResponseDTO::new);
    }

    public Optional<Card> getCardById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return cardRepository.findById(id);
    }

    public Card requireCardById(String id) {
        if (id == null || id.isBlank()) {
            throw new ResourceNotFoundException("CARD_NOT_FOUND", "Card not found.");
        }
        return cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CARD_NOT_FOUND", "Card not found."));
    }

    public Card requireCardByContext(String cardId, String listId, String boardId) {
        Card card = requireCardById(cardId);
        if (!card.getList().getId().equals(listId) || !card.getList().getBoard().getId().equals(boardId)) {
            throw new ResourceNotFoundException("CARD_NOT_FOUND", "Card not found in this list/board.");
        }
        return card;
    }

    @Transactional
    public CardResponseDTO updateCard(Card card, UpdateCardDTO dto) {
        if (dto.name() != null) card.setName(dto.name());
        if (dto.description() != null) card.setDescription(dto.description());
        if (dto.status() != null) card.setStatus(dto.status());
        if (dto.position() != null) card.setPosition(dto.position());
        card.setUpdatedAt(LocalDateTime.now());
        
        return new CardResponseDTO(cardRepository.save(card));
    }

    @Transactional
    public CardResponseDTO moveCard(Card card, BoardList targetList, MoveCardDTO dto) {
        card.setList(targetList);

        if (dto.position() != null) {
            card.setPosition(dto.position());
        } else {
            Integer maxPosition = cardRepository.findMaxPositionByList(targetList);
            card.setPosition(maxPosition == null ? 0 : maxPosition + 1);
        }

        card.setUpdatedAt(LocalDateTime.now());
        return new CardResponseDTO(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(String id) {
        if (id != null && !id.isBlank()) {
            cardRepository.findById(id).ifPresent(card -> {
                attachmentService.deleteAllByCard(card);
                cardRepository.deleteById(id);
            });
        }
    }

    @Transactional
    public void deleteAllByList(BoardList list) {
        List<Card> cards = cardRepository.findByListOrderByPositionAsc(list);
        for (Card card : cards) {
            attachmentService.deleteAllByCard(card);
        }
        cardRepository.deleteByList(list);
    }
}
