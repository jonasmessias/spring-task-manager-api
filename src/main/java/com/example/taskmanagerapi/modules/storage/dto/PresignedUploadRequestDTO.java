package com.example.taskmanagerapi.modules.storage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PresignedUploadRequestDTO(
        @NotBlank(message = "File name is required")
        String fileName,

        @NotBlank(message = "Content type is required")
        String contentType,

        @Min(value = 1, message = "File size must be greater than 0")
        long fileSize
) {}
