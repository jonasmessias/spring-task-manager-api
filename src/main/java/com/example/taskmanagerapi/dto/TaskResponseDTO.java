package com.example.taskmanagerapi.dto;

import java.time.LocalDateTime;

import com.example.taskmanagerapi.domain.task.Task;
import com.example.taskmanagerapi.domain.task.TaskStatus;

public record TaskResponseDTO(
    String id,
    String title,
    String description,
    TaskStatus status,
    String userId,
    String userName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public TaskResponseDTO(Task task) {
        this(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getUser().getId(),
            task.getUser().getName(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
