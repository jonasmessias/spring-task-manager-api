package com.example.taskmanagerapi.modules.cards.dto;

import java.time.LocalDateTime;

import com.example.taskmanagerapi.modules.cards.domain.Card;
import com.example.taskmanagerapi.modules.cards.domain.CardStatus;

public record CardResponseDTO(
    String id,
    String name,
    String description,
    CardStatus status,
    Integer position,
    String listId,
    String listName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public CardResponseDTO(Card card) {
        this(
            card.getId(),
            card.getName(),
            card.getDescription(),
            card.getStatus(),
            card.getPosition(),
            card.getList().getId(),
            card.getList().getName(),
            card.getCreatedAt(),
            card.getUpdatedAt()
        );
    }
}
