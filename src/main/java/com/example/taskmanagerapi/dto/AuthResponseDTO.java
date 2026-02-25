package com.example.taskmanagerapi.dto;

public record AuthResponseDTO(
    String name,
    String accessToken,
    String refreshToken
) {
}
