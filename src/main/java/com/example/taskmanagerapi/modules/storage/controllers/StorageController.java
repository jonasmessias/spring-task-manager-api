package com.example.taskmanagerapi.modules.storage.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.taskmanagerapi.modules.storage.dto.PresignedUploadRequestDTO;
import com.example.taskmanagerapi.modules.storage.dto.PresignedUploadResponseDTO;
import com.example.taskmanagerapi.modules.storage.dto.UploadResponseDTO;
import com.example.taskmanagerapi.modules.storage.services.StorageService;
import com.example.taskmanagerapi.modules.storage.services.StorageService.PresignedUploadResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * StorageController - REST endpoints for file upload operations.
 * Provides two upload strategies:
 *   1. Direct upload via API (small files)
 *   2. Presigned URL generation (large files)
 */
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "Endpoints for file upload and management")
@SecurityRequirement(name = "Bearer Authentication")
public class StorageController {

    private final StorageService storageService;

    @Operation(summary = "Upload file", description = "Upload a file directly via the API. Accepts JPG, PNG and WebP images up to 5MB.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file (wrong type or too large)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder
    ) {
        String fileUrl = storageService.uploadFile(file, folder);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponseDTO(fileUrl));
    }

    @Operation(summary = "Request presigned upload URL", description = "Generate a presigned URL for direct upload to S3 from the frontend. Supports files up to 50MB. Blocks executable files.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Presigned URL generated"),
            @ApiResponse(responseCode = "400", description = "Invalid file name, type or size"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/presigned-upload")
    public ResponseEntity<PresignedUploadResponseDTO> getPresignedUploadUrl(
            @Valid @RequestBody PresignedUploadRequestDTO request,
            @RequestParam(value = "folder", defaultValue = "attachments") String folder
    ) {
        PresignedUploadResult result = storageService.generatePresignedUploadUrl(
                request.fileName(),
                request.contentType(),
                request.fileSize(),
                folder
        );

        return ResponseEntity.ok(new PresignedUploadResponseDTO(
                result.uploadUrl(),
                result.fileKey(),
                result.fileUrl()
        ));
    }

    @Operation(summary = "Delete file", description = "Delete a file from S3 by its URL or key.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "File deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteFile(@RequestParam("fileUrl") String fileUrl) {
        storageService.deleteFile(fileUrl);
        return ResponseEntity.noContent().build();
    }
}
