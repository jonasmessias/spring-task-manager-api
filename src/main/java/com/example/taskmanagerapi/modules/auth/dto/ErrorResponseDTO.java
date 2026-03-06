package com.example.taskmanagerapi.modules.auth.dto;

import java.time.Instant;

public record ErrorResponseDTO(String code, String message, int statusCode, Instant timestamp) {
    public ErrorResponseDTO(String code, String message, int statusCode) {
        this(code, message, statusCode, Instant.now());
    }
}
