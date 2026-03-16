package com.example.taskmanagerapi.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleTokenRequestDTO(
    @NotBlank String idToken
) {}
