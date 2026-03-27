package com.example.taskmanagerapi.modules.cards.dto;

import java.time.LocalDateTime;

import com.example.taskmanagerapi.modules.cards.domain.Attachment;

public record AttachmentResponseDTO(
        String id,
        String fileName,
        String fileKey,
        String fileUrl,
        String contentType,
        long fileSize,
        String cardId,
        String uploadedById,
        LocalDateTime createdAt
) {
    public AttachmentResponseDTO(Attachment attachment) {
        this(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFileKey(),
                attachment.getFileUrl(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getCard().getId(),
                attachment.getUploadedBy(),
                attachment.getCreatedAt()
        );
    }
}
