package com.example.taskmanagerapi.modules.cards.dto;

public record PresignedUrlResponseDTO(
        String uploadUrl,
        String fileKey,
        String fileUrl
) {}
