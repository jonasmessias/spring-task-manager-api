package com.example.taskmanagerapi.modules.cards.dto;

import com.example.taskmanagerapi.modules.cards.domain.CardStatus;

public record CreateCardDTO(
    String name,
    String description,
    CardStatus status,
    Integer position
) {}
