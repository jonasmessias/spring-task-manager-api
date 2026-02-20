package com.example.taskmanagerapi.dto;

import com.example.taskmanagerapi.domain.task.TaskStatus;

public record UpdateTaskDTO(
    String title,
    String description,
    TaskStatus status
) {}
