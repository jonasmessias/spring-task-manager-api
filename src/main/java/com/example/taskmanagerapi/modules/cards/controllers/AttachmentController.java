package com.example.taskmanagerapi.modules.cards.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.boards.services.BoardService;
import com.example.taskmanagerapi.modules.cards.domain.Card;
import com.example.taskmanagerapi.modules.cards.dto.AttachmentConfirmDTO;
import com.example.taskmanagerapi.modules.cards.dto.AttachmentRequestDTO;
import com.example.taskmanagerapi.modules.cards.dto.AttachmentResponseDTO;
import com.example.taskmanagerapi.modules.cards.dto.PresignedUrlResponseDTO;
import com.example.taskmanagerapi.modules.cards.services.AttachmentService;
import com.example.taskmanagerapi.modules.cards.services.CardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cards/{cardId}/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Endpoints for managing file attachments on cards")
@SecurityRequirement(name = "Bearer Authentication")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final CardService cardService;
    private final BoardService boardService;

    @Operation(summary = "Request Upload URL",
            description = "Generate a presigned URL for direct upload to S3.")
    @PostMapping("/request-upload")
    public ResponseEntity<PresignedUrlResponseDTO> requestUpload(
            @PathVariable @NonNull String cardId,
            @Valid @RequestBody AttachmentRequestDTO body,
            @AuthenticationPrincipal User user) {

        Card card = cardService.requireCardById(cardId);
        boardService.requireMember(card.getList().getBoard(), user);

        PresignedUrlResponseDTO response = attachmentService.requestUpload(
                body.fileName(), body.contentType(), body.fileSize()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Confirm Upload",
            description = "After the frontend uploads to S3, call this to save the attachment metadata.")
    @PostMapping("/confirm")
    public ResponseEntity<AttachmentResponseDTO> confirmUpload(
            @PathVariable @NonNull String cardId,
            @Valid @RequestBody AttachmentConfirmDTO body,
            @AuthenticationPrincipal User user) {

        Card card = cardService.requireCardById(cardId);
        boardService.requireMember(card.getList().getBoard(), user);

        AttachmentResponseDTO response = attachmentService.confirmUpload(body, card, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List Attachments", description = "Get all attachments for a card, ordered by newest first.")
    @GetMapping
    public ResponseEntity<List<AttachmentResponseDTO>> listAttachments(
            @PathVariable @NonNull String cardId,
            @AuthenticationPrincipal User user) {

        Card card = cardService.requireCardById(cardId);
        boardService.requireMember(card.getList().getBoard(), user);

        return ResponseEntity.ok(attachmentService.getAttachmentsByCard(card));
    }

    @Operation(summary = "Delete Attachment", description = "Delete an attachment from a card and S3.")
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable @NonNull String cardId,
            @PathVariable @NonNull String attachmentId,
            @AuthenticationPrincipal User user) {

        Card card = cardService.requireCardById(cardId);
        boardService.requireMember(card.getList().getBoard(), user);

        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }
}
