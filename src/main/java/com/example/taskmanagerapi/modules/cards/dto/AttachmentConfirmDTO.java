package com.example.taskmanagerapi.modules.cards.dto;

import jakarta.validation.constraints.NotBlank;

public record AttachmentConfirmDTO(
        @NotBlank(message = "File key is required")
        String fileKey,

        @NotBlank(message = "File name is required")
        String fileName,

        @NotBlank(message = "Content type is required")
        String contentType,

        long fileSize
) {}
