package com.example.taskmanagerapi.modules.tasks.dto;

import java.time.LocalDateTime;

import com.example.taskmanagerapi.modules.tasks.domain.Task;
import com.example.taskmanagerapi.modules.tasks.domain.TaskStatus;

public record TaskResponseDTO(
    String id,
    String title,
    String description,
    TaskStatus status,
    String ownerId,
    String ownerName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public TaskResponseDTO(Task task) {
        this(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getOwner().getId(),
            task.getOwner().getName(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
