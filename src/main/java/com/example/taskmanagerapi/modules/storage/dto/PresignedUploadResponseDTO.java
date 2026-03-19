package com.example.taskmanagerapi.modules.storage.dto;

public record PresignedUploadResponseDTO(String uploadUrl, String fileKey, String fileUrl) {}
