package com.example.taskmanagerapi.modules.tasks.dto;

import com.example.taskmanagerapi.modules.tasks.domain.TaskStatus;

public record UpdateTaskDTO(
    String title,
    String description,
    TaskStatus status
) {}
