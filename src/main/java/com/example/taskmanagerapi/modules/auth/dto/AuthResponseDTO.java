package com.example.taskmanagerapi.modules.auth.dto;

public record AuthResponseDTO(
    String name,
    String accessToken,
    String refreshToken
) {}
