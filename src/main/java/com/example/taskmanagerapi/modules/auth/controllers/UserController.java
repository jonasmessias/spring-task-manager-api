package com.example.taskmanagerapi.modules.auth.controllers;

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
import com.example.taskmanagerapi.modules.auth.services.UserService;
import com.example.taskmanagerapi.modules.storage.dto.UploadResponseDTO;

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

    private final UserService userService;

    @Operation(summary = "Get Current User", description = "Get information about the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user information",
                content = @Content(schema = @Schema(implementation = UserProfileDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getProfile(user));
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
    public ResponseEntity<UserProfileDTO> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileDTO body) {
        return ResponseEntity.ok(userService.updateProfile(user, body));
    }

    @Operation(summary = "Delete Account", description = "Permanently delete the authenticated user's account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user) {
        userService.deleteAccount(user);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload Avatar", description = "Upload or replace the current user's avatar. Accepts JPG, PNG and WebP images up to 5MB.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully",
                content = @Content(schema = @Schema(implementation = UploadResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file (wrong type or too large)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDTO> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        String fileUrl = userService.uploadAvatar(user, file);
        return ResponseEntity.ok(new UploadResponseDTO(fileUrl));
    }

    @Operation(summary = "Delete Avatar", description = "Remove the current user's avatar.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Avatar removed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal User user) {
        userService.deleteAvatar(user);
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
    public ResponseEntity<UserProfileDTO> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable @NonNull String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
