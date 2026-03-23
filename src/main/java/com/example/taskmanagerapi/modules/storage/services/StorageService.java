package com.example.taskmanagerapi.modules.storage.services;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.taskmanagerapi.infra.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    );

    private static final List<String> BLOCKED_EXTENSIONS = Arrays.asList(
            ".exe", ".bat", ".cmd", ".sh", ".ps1", ".msi", ".dll", ".com"
    );

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;       // 5MB
    private static final long MAX_ATTACHMENT_SIZE = 50 * 1024 * 1024;  // 50MB

    // ── Direct Upload (avatars, covers) ─────────────────────────────────

    public String uploadFile(MultipartFile file, String folder) {
        validateImage(file);

        String key = generateKey(folder, file.getOriginalFilename());

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            String url = buildFileUrl(key);
            log.info("File uploaded successfully: {}", url);
            return url;

        } catch (IOException e) {
            throw new BusinessException("FILE_UPLOAD_ERROR", "Failed to upload file to S3.");
        }
    }

    // ── Presigned URL (attachments) ─────────────────────────────────────

    public PresignedUploadResult generatePresignedUploadUrl(String fileName, String contentType, long fileSize, String folder) {
        validateAttachment(fileName, fileSize);

        String key = generateKey(folder, fileName);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        log.info("Presigned URL generated for: {}", key);
        return new PresignedUploadResult(
                presignedRequest.url().toString(),
                key,
                buildFileUrl(key)
        );
    }

    // ── Delete ──────────────────────────────────────────────────────────

    public void deleteFile(String fileUrlOrKey) {
        String key = fileUrlOrKey.contains("amazonaws.com")
                ? extractKeyFromUrl(fileUrlOrKey)
                : fileUrlOrKey;

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(request);
        log.info("File deleted from S3: {}", key);
    }

    // ── Validation ──────────────────────────────────────────────────────

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only JPG, PNG and WebP images are allowed");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image size must not exceed 5MB");
        }
    }

    private void validateAttachment(String fileName, long fileSize) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        if (fileSize > MAX_ATTACHMENT_SIZE) {
            throw new IllegalArgumentException("Attachment size must not exceed 50MB");
        }
        String lowerName = fileName.toLowerCase();
        for (String ext : BLOCKED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                throw new IllegalArgumentException("File type not allowed: " + ext);
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String generateKey(String folder, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return folder + "/" + UUID.randomUUID() + extension;
    }

    public String buildPublicUrl(String key) {
        return buildFileUrl(key);
    }

    private String buildFileUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

    private String extractKeyFromUrl(String url) {
        // https://bucket.s3.region.amazonaws.com/folder/file.ext → folder/file.ext
        return url.substring(url.indexOf(".com/") + 5);
    }

    // ── Result record ───────────────────────────────────────────────────

    public record PresignedUploadResult(String uploadUrl, String fileKey, String fileUrl) {}
}
