package com.example.taskmanagerapi.modules.cards.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.modules.cards.domain.Card;
import com.example.taskmanagerapi.modules.cards.domain.CardStatus;
import com.example.taskmanagerapi.modules.cards.dto.CardResponseDTO;
import com.example.taskmanagerapi.modules.cards.dto.CreateCardDTO;
import com.example.taskmanagerapi.modules.cards.dto.UpdateCardDTO;
import com.example.taskmanagerapi.modules.cards.repositories.CardRepository;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;

import lombok.RequiredArgsConstructor;

/**
 * CardService - Business logic for card operations
 * Single Responsibility: Handle all card-related business logic
 */
@Service
@RequiredArgsConstructor
public class CardService {
    
    private final CardRepository cardRepository;
    
    /**
     * Create a new card in a list
     */
    @Transactional
    public CardResponseDTO createCard(CreateCardDTO dto, BoardList list) {
        Card card = new Card();
        card.setName(dto.name());
        card.setDescription(dto.description());
        card.setStatus(dto.status() != null ? dto.status() : CardStatus.ACTIVE);
        card.setPosition(dto.position());
        card.setList(list);
        card.setCreatedAt(LocalDateTime.now());
        
        Card savedCard = cardRepository.save(card);
        return new CardResponseDTO(savedCard);
    }
    
    /**
     * Get all cards from a list
     */
    public List<CardResponseDTO> getCardsByList(BoardList list) {
        List<Card> cards = cardRepository.findByListOrderByPositionAsc(list);
        return cards.stream()
                .map(CardResponseDTO::new)
                .toList();
    }
    
    /**
     * Get card by ID
     */
    public Optional<Card> getCardById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return cardRepository.findById(id);
    }
    
    /**
     * Update a card
     */
    @Transactional
    public CardResponseDTO updateCard(Card card, UpdateCardDTO dto) {
        if (dto.name() != null) {
            card.setName(dto.name());
        }
        if (dto.description() != null) {
            card.setDescription(dto.description());
        }
        if (dto.status() != null) {
            card.setStatus(dto.status());
        }
        if (dto.position() != null) {
            card.setPosition(dto.position());
        }
        card.setUpdatedAt(LocalDateTime.now());
        
        Card updatedCard = cardRepository.save(card);
        return new CardResponseDTO(updatedCard);
    }
    
    /**
     * Delete a card
     */
    @Transactional
    public void deleteCard(String id) {
        if (id != null && !id.isBlank()) {
            cardRepository.deleteById(id);
        }
    }
    
    /**
     * Delete all cards from a list
     * Used when deleting a list
     */
    @Transactional
    public void deleteAllByList(BoardList list) {
        cardRepository.deleteByList(list);
    }
}
