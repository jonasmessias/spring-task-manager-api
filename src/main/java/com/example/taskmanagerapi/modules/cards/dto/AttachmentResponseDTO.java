package com.example.taskmanagerapi.modules.cards.dto;

import java.time.LocalDateTime;

import com.example.taskmanagerapi.modules.cards.domain.Attachment;

public record AttachmentResponseDTO(
        String id,
        String fileName,
        String fileUrl,
        String contentType,
        long fileSize,
        String uploadedBy,
        LocalDateTime createdAt
) {
    public AttachmentResponseDTO(Attachment attachment) {
        this(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFileUrl(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getUploadedBy(),
                attachment.getCreatedAt()
        );
    }
}
