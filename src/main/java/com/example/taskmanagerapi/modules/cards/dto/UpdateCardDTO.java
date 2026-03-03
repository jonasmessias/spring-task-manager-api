package com.example.taskmanagerapi.modules.cards.dto;

import com.example.taskmanagerapi.modules.cards.domain.CardStatus;

public record UpdateCardDTO(
    String name,
    String description,
    CardStatus status,
    Integer position
) {}
