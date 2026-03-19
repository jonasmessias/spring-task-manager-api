package com.example.taskmanagerapi.modules.auth.controllers;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.dto.UpdateProfileDTO;
import com.example.taskmanagerapi.modules.auth.dto.UserProfileDTO;
import com.example.taskmanagerapi.modules.auth.repositories.UserRepository;
import com.example.taskmanagerapi.modules.storage.dto.UploadResponseDTO;
import com.example.taskmanagerapi.modules.storage.services.StorageService;

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

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {
    
    private final UserRepository userRepository;
    private final StorageService storageService;

    private static final String AVATAR_FOLDER = "avatars";

    @Operation(summary = "Get Current User", description = "Get information about the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user information",
                content = @Content(schema = @Schema(implementation = UserProfileDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @GetMapping("/me")
    public ResponseEntity<Object> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new UserProfileDTO(
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.getEmail(),
            user.getAvatarUrl()
        ));
    }

    @Operation(summary = "Update Profile", description = "Update the current user's name and/or username")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                content = @Content(schema = @Schema(implementation = UserProfileDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "409", description = "Username already taken"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @PutMapping("/me")
    public ResponseEntity<Object> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileDTO body) {

        // Check username uniqueness if it's being changed
        if (!user.getUsername().equals(body.username())) {
            Optional<User> existing = userRepository.findByUsername(body.username());
            if (existing.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Username '" + body.username() + "' is already taken");
            }
        }

        if (body.name() != null && !body.name().isBlank()) {
            user.setName(body.name());
        }
        if (body.username() != null && !body.username().isBlank()) {
            user.setUsername(body.username());
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(new UserProfileDTO(
            saved.getId(),
            saved.getName(),
            saved.getUsername(),
            saved.getEmail(),
            saved.getAvatarUrl()
        ));
    }

    @Operation(summary = "Delete Account", description = "Permanently delete the authenticated user's account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user) {
        // Delete avatar from S3 if exists
        if (user.getAvatarUrl() != null) {
            storageService.deleteFile(user.getAvatarUrl());
        }
        String userId = user.getId();
        if (userId != null) {
            userRepository.deleteById(userId);
        }
        return ResponseEntity.noContent().build();
    }

    // ── Avatar endpoints ────────────────────────────────────────────────

    @Operation(summary = "Upload Avatar", description = "Upload or replace the current user's avatar. Accepts JPG, PNG and WebP images up to 5MB. Send null/empty to use default.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully",
                content = @Content(schema = @Schema(implementation = UploadResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file (wrong type or too large)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDTO> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file
    ) {
        // Delete old avatar if exists
        if (user.getAvatarUrl() != null) {
            storageService.deleteFile(user.getAvatarUrl());
        }

        String fileUrl = storageService.uploadFile(file, AVATAR_FOLDER);
        user.setAvatarUrl(fileUrl);
        userRepository.save(user);

        return ResponseEntity.ok(new UploadResponseDTO(fileUrl));
    }

    @Operation(summary = "Delete Avatar", description = "Remove the current user's avatar. The frontend should then display the default avatar.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Avatar removed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal User user) {
        if (user.getAvatarUrl() != null) {
            storageService.deleteFile(user.getAvatarUrl());
            user.setAvatarUrl(null);
            userRepository.save(user);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get User by ID", description = "Get public information about a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user",
                content = @Content(schema = @Schema(implementation = UserProfileDTO.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Object> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable @NonNull String id) {

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User u = userOpt.get();
        return ResponseEntity.ok(new UserProfileDTO(
            u.getId(),
            u.getName(),
            u.getUsername(),
            u.getEmail(),
            u.getAvatarUrl()
        ));
    }
}
