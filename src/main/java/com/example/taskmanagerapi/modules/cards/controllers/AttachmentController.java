package com.example.taskmanagerapi.modules.cards.controllers;

import java.util.List;
import java.util.Optional;

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
import com.example.taskmanagerapi.modules.auth.dto.ErrorResponseDTO;
import com.example.taskmanagerapi.modules.boards.services.BoardMemberService;
import com.example.taskmanagerapi.modules.cards.domain.Card;
import com.example.taskmanagerapi.modules.cards.dto.AttachmentConfirmDTO;
import com.example.taskmanagerapi.modules.cards.dto.AttachmentRequestDTO;
import com.example.taskmanagerapi.modules.cards.dto.AttachmentResponseDTO;
import com.example.taskmanagerapi.modules.cards.dto.PresignedUrlResponseDTO;
import com.example.taskmanagerapi.modules.cards.services.AttachmentService;
import com.example.taskmanagerapi.modules.cards.services.CardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * AttachmentController - REST endpoints for card file attachments.
 * Uses presigned URLs: the frontend uploads directly to S3, then confirms here.
 */
@RestController
@RequestMapping("/cards/{cardId}/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Endpoints for managing file attachments on cards")
@SecurityRequirement(name = "Bearer Authentication")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final CardService cardService;
    private final BoardMemberService boardMemberService;

    // ── Step 1: Request presigned upload URL ────────────────────────────

    @Operation(summary = "Request Upload URL",
            description = "Generate a presigned URL for direct upload to S3. " +
                    "The frontend uses the returned `uploadUrl` to PUT the file directly to S3, " +
                    "then calls POST /confirm with the `fileKey`.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Presigned URL generated",
                content = @Content(schema = @Schema(implementation = PresignedUrlResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Card not found"),
        @ApiResponse(responseCode = "403", description = "Not a board member"),
        @ApiResponse(responseCode = "400", description = "Invalid file metadata"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/request-upload")
    public ResponseEntity<Object> requestUpload(
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @Valid @RequestBody AttachmentRequestDTO body,
            @AuthenticationPrincipal User user) {

        Optional<Card> cardOpt = cardService.getCardById(cardId);
        if (cardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                    "CARD_NOT_FOUND", "Card not found.", 404
            ));
        }

        Card card = cardOpt.get();
        if (!boardMemberService.isMember(card.getList().getBoard(), user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                    "FORBIDDEN", "You don't have permission to upload attachments to this card.", 403
            ));
        }

        PresignedUrlResponseDTO response = attachmentService.requestUpload(
                body.fileName(), body.contentType(), body.fileSize()
        );
        return ResponseEntity.ok(response);
    }

    // ── Step 2: Confirm upload ──────────────────────────────────────────

    @Operation(summary = "Confirm Upload",
            description = "After the frontend uploads the file to S3 using the presigned URL, " +
                    "call this endpoint to save the attachment metadata in the database.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Attachment saved",
                content = @Content(schema = @Schema(implementation = AttachmentResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Card not found"),
        @ApiResponse(responseCode = "403", description = "Not a board member"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/confirm")
    public ResponseEntity<Object> confirmUpload(
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @Valid @RequestBody AttachmentConfirmDTO body,
            @AuthenticationPrincipal User user) {

        Optional<Card> cardOpt = cardService.getCardById(cardId);
        if (cardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                    "CARD_NOT_FOUND", "Card not found.", 404
            ));
        }

        Card card = cardOpt.get();
        if (!boardMemberService.isMember(card.getList().getBoard(), user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                    "FORBIDDEN", "You don't have permission to add attachments to this card.", 403
            ));
        }

        AttachmentResponseDTO response = attachmentService.confirmUpload(body, card, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ── List attachments ────────────────────────────────────────────────

    @Operation(summary = "List Attachments", description = "Get all attachments for a card, ordered by newest first.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Attachments retrieved"),
        @ApiResponse(responseCode = "404", description = "Card not found"),
        @ApiResponse(responseCode = "403", description = "Not a board member"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Object> listAttachments(
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @AuthenticationPrincipal User user) {

        Optional<Card> cardOpt = cardService.getCardById(cardId);
        if (cardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                    "CARD_NOT_FOUND", "Card not found.", 404
            ));
        }

        Card card = cardOpt.get();
        if (!boardMemberService.isMember(card.getList().getBoard(), user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                    "FORBIDDEN", "You don't have permission to view attachments on this card.", 403
            ));
        }

        List<AttachmentResponseDTO> response = attachmentService.getAttachmentsByCard(card);
        return ResponseEntity.ok(response);
    }

    // ── Delete attachment ───────────────────────────────────────────────

    @Operation(summary = "Delete Attachment", description = "Delete an attachment from a card. Removes the file from S3 and the record from the database.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Attachment deleted"),
        @ApiResponse(responseCode = "404", description = "Card or attachment not found"),
        @ApiResponse(responseCode = "403", description = "Not a board member"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Object> deleteAttachment(
            @Parameter(description = "Card ID", required = true) @PathVariable @NonNull String cardId,
            @Parameter(description = "Attachment ID", required = true) @PathVariable @NonNull String attachmentId,
            @AuthenticationPrincipal User user) {

        Optional<Card> cardOpt = cardService.getCardById(cardId);
        if (cardOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                    "CARD_NOT_FOUND", "Card not found.", 404
            ));
        }

        Card card = cardOpt.get();
        if (!boardMemberService.isMember(card.getList().getBoard(), user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                    "FORBIDDEN", "You don't have permission to delete attachments from this card.", 403
            ));
        }

        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }
}
