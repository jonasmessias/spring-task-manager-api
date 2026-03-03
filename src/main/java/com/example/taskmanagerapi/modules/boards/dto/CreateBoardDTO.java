package com.example.taskmanagerapi.modules.boards.dto;

import com.example.taskmanagerapi.modules.boards.domain.BoardType;

public record CreateBoardDTO(
    String name,
    BoardType type,
    String description
) {}
