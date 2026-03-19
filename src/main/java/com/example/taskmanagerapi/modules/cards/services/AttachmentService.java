package com.example.taskmanagerapi.modules.cards.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.modules.cards.domain.Attachment;
import com.example.taskmanagerapi.modules.cards.domain.Card;
import com.example.taskmanagerapi.modules.cards.dto.AttachmentConfirmDTO;
import com.example.taskmanagerapi.modules.cards.dto.AttachmentResponseDTO;
import com.example.taskmanagerapi.modules.cards.dto.PresignedUrlResponseDTO;
import com.example.taskmanagerapi.modules.cards.repositories.AttachmentRepository;
import com.example.taskmanagerapi.modules.storage.services.StorageService;
import com.example.taskmanagerapi.modules.storage.services.StorageService.PresignedUploadResult;

import lombok.RequiredArgsConstructor;

/**
 * AttachmentService - Business logic for card attachments.
 * Uses presigned URLs so files upload directly to S3 from the frontend.
 */
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final StorageService storageService;

    private static final String ATTACHMENTS_FOLDER = "attachments";

    /**
     * Step 1: Generate a presigned upload URL for the frontend.
     * The API validates permissions and file metadata, then returns a temporary URL.
     */
    public PresignedUrlResponseDTO requestUpload(String fileName, String contentType, long fileSize) {
        PresignedUploadResult result = storageService.generatePresignedUploadUrl(
                fileName, contentType, fileSize, ATTACHMENTS_FOLDER
        );
        return new PresignedUrlResponseDTO(
                result.uploadUrl(),
                result.fileKey(),
                result.fileUrl()
        );
    }

    /**
     * Step 2: Confirm the upload after the frontend has uploaded directly to S3.
     * Creates the Attachment record in the database.
     */
    @Transactional
    public AttachmentResponseDTO confirmUpload(AttachmentConfirmDTO dto, Card card, String userId) {
        Attachment attachment = new Attachment();
        attachment.setFileName(dto.fileName());
        attachment.setFileUrl(storageService.buildPublicUrl(dto.fileKey()));
        attachment.setFileKey(dto.fileKey());
        attachment.setContentType(dto.contentType());
        attachment.setFileSize(dto.fileSize());
        attachment.setCard(card);
        attachment.setUploadedBy(userId);

        Attachment saved = attachmentRepository.save(attachment);
        return new AttachmentResponseDTO(saved);
    }

    /**
     * List all attachments for a card.
     */
    public List<AttachmentResponseDTO> getAttachmentsByCard(Card card) {
        return attachmentRepository.findByCardOrderByCreatedAtDesc(card)
                .stream()
                .map(AttachmentResponseDTO::new)
                .toList();
    }

    /**
     * Count attachments for a card.
     */
    public long countByCard(Card card) {
        return attachmentRepository.countByCard(card);
    }

    /**
     * Delete a single attachment (removes from S3 + database).
     */
    @Transactional
    public void deleteAttachment(String attachmentId) {
        Optional<Attachment> opt = attachmentRepository.findById(attachmentId);
        if (opt.isPresent()) {
            Attachment attachment = opt.get();
            storageService.deleteFile(attachment.getFileKey());
            attachmentRepository.delete(attachment);
        }
    }

    /**
     * Delete all attachments for a card (used when deleting a card).
     */
    @Transactional
    public void deleteAllByCard(Card card) {
        List<Attachment> attachments = attachmentRepository.findByCardOrderByCreatedAtDesc(card);
        for (Attachment attachment : attachments) {
            storageService.deleteFile(attachment.getFileKey());
        }
        attachmentRepository.deleteByCard(card);
    }
}
