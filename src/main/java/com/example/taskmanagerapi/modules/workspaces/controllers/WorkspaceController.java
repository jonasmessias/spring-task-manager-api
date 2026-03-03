package com.example.taskmanagerapi.modules.workspaces.controllers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;
import com.example.taskmanagerapi.modules.workspaces.dto.CreateWorkspaceDTO;
import com.example.taskmanagerapi.modules.workspaces.dto.UpdateWorkspaceDTO;
import com.example.taskmanagerapi.modules.workspaces.dto.WorkspaceDetailDTO;
import com.example.taskmanagerapi.modules.workspaces.dto.WorkspaceResponseDTO;
import com.example.taskmanagerapi.modules.workspaces.services.WorkspaceService;

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
@RequestMapping("/workspaces")
@RequiredArgsConstructor
@Tag(name = "Workspaces", description = "Endpoints for managing workspaces - Top-level containers for boards")
@SecurityRequirement(name = "Bearer Authentication")
public class WorkspaceController {
    
    private final WorkspaceService workspaceService;

    @Operation(summary = "Create Workspace", description = "Create a new workspace for organizing boards")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Workspace created successfully",
                content = @Content(schema = @Schema(implementation = WorkspaceResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or workspace name already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping
    public ResponseEntity<Object> createWorkspace(
            @Valid @RequestBody CreateWorkspaceDTO body,
            @AuthenticationPrincipal User user) {
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        
        try {
            WorkspaceResponseDTO response = workspaceService.createWorkspace(
                Objects.requireNonNull(body, "Request body cannot be null"), 
                Objects.requireNonNull(user, "User cannot be null")
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Get All Workspaces", description = "Retrieve all workspaces for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspaces retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    public ResponseEntity<Object> getAllWorkspaces(@AuthenticationPrincipal User user) {
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        
        List<WorkspaceResponseDTO> response = workspaceService.getWorkspacesByUser(user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Workspace by ID", description = "Retrieve a specific workspace with all its boards")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace found",
                content = @Content(schema = @Schema(implementation = WorkspaceDetailDTO.class))),
        @ApiResponse(responseCode = "404", description = "Workspace not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Workspace belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Object> getWorkspaceById(
            @Parameter(description = "Workspace ID", required = true) @PathVariable String id,
            @AuthenticationPrincipal User user) {
        
        Optional<Workspace> workspaceOpt = workspaceService.getWorkspaceById(
            Objects.requireNonNull(id, "Workspace ID cannot be null")
        );
        
        if (workspaceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Workspace not found");
        }
        
        Workspace workspace = workspaceOpt.get();
        
        // Check if workspace belongs to the authenticated user
        if (!workspaceService.isWorkspaceOwner(
                Objects.requireNonNull(workspace, "Workspace cannot be null"), 
                Objects.requireNonNull(user, "User cannot be null"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to access this workspace");
        }
        
        WorkspaceDetailDTO response = new WorkspaceDetailDTO(workspace);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update Workspace", description = "Update workspace name or description")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace updated successfully",
                content = @Content(schema = @Schema(implementation = WorkspaceResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Workspace not found"),
        @ApiResponse(responseCode = "400", description = "Invalid request or workspace name already exists"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Workspace belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateWorkspace(
            @Parameter(description = "Workspace ID", required = true) @PathVariable String id,
            @Valid @RequestBody UpdateWorkspaceDTO body,
            @AuthenticationPrincipal User user) {
        
        Optional<Workspace> workspaceOpt = workspaceService.getWorkspaceById(
            Objects.requireNonNull(id, "Workspace ID cannot be null")
        );
        
        if (workspaceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Workspace not found");
        }
        
        Workspace workspace = workspaceOpt.get();
        
        // Check if workspace belongs to the authenticated user
        if (!workspaceService.isWorkspaceOwner(
                Objects.requireNonNull(workspace, "Workspace cannot be null"), 
                Objects.requireNonNull(user, "User cannot be null"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to update this workspace");
        }
        
        try {
            WorkspaceResponseDTO response = workspaceService.updateWorkspace(
                Objects.requireNonNull(workspace, "Workspace cannot be null"), 
                Objects.requireNonNull(body, "Request body cannot be null")
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Delete Workspace", description = "Delete a workspace and all its boards, lists, and cards. User can delete all workspaces if desired.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Workspace deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Workspace not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Workspace belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteWorkspace(
            @Parameter(description = "Workspace ID", required = true) @PathVariable String id,
            @AuthenticationPrincipal User user) {
        
        Optional<Workspace> workspaceOpt = workspaceService.getWorkspaceById(
            Objects.requireNonNull(id, "Workspace ID cannot be null")
        );
        
        if (workspaceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Workspace not found");
        }
        
        Workspace workspace = workspaceOpt.get();
        
        // Check if workspace belongs to the authenticated user
        if (!workspaceService.isWorkspaceOwner(
                Objects.requireNonNull(workspace, "Workspace cannot be null"), 
                Objects.requireNonNull(user, "User cannot be null"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to delete this workspace");
        }
        
        try {
            workspaceService.deleteWorkspace(
                Objects.requireNonNull(id, "Workspace ID cannot be null")
            );
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
