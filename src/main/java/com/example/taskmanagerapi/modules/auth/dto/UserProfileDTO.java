package com.example.taskmanagerapi.modules.auth.dto;

public record UserProfileDTO(
    String id,
    String name,
    String email
) {}
