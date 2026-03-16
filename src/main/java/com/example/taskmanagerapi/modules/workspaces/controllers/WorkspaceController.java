package com.example.taskmanagerapi.modules.workspaces.controllers;

import java.util.List;
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
import com.example.taskmanagerapi.modules.auth.dto.ErrorResponseDTO;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;
import com.example.taskmanagerapi.modules.workspaces.dto.CreateWorkspaceDTO;
import com.example.taskmanagerapi.modules.workspaces.dto.UpdateWorkspaceDTO;
import com.example.taskmanagerapi.modules.workspaces.dto.WorkspaceDetailDTO;
import com.example.taskmanagerapi.modules.workspaces.dto.WorkspaceResponseDTO;
import com.example.taskmanagerapi.modules.workspaces.services.WorkspaceMemberService;
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
    private final WorkspaceMemberService memberService;

    @Operation(summary = "Create Workspace", description = "Create a new workspace for organizing boards")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Workspace created successfully",
                content = @Content(schema = @Schema(implementation = WorkspaceResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Workspace name already exists — `WORKSPACE_NAME_EXISTS`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping
    public ResponseEntity<Object> createWorkspace(
            @Valid @RequestBody CreateWorkspaceDTO body,
            @AuthenticationPrincipal User user) {
        
        try {
            WorkspaceResponseDTO response = workspaceService.createWorkspace(body, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(
                "WORKSPACE_NAME_EXISTS", e.getMessage(), 400
            ));
        }
    }

    @Operation(summary = "Get All Workspaces", description = "Retrieve all workspaces the authenticated user owns or was invited to")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspaces retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    public ResponseEntity<List<WorkspaceResponseDTO>> getAllWorkspaces(@AuthenticationPrincipal User user) {
        List<WorkspaceResponseDTO> response = workspaceService.getWorkspacesByUser(user);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Workspace by ID", description = "Retrieve a specific workspace with all its boards")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace found",
                content = @Content(schema = @Schema(implementation = WorkspaceDetailDTO.class))),
        @ApiResponse(responseCode = "404", description = "Workspace not found — `WORKSPACE_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not a member of this workspace — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Object> getWorkspaceById(
            @Parameter(description = "Workspace ID", required = true) @PathVariable String id,
            @AuthenticationPrincipal User user) {
        
        Optional<Workspace> workspaceOpt = workspaceService.getWorkspaceById(id);
        
        if (workspaceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "WORKSPACE_NOT_FOUND", "Workspace not found.", 404
            ));
        }
        
        Workspace workspace = workspaceOpt.get();
        
        if (!memberService.isMember(workspace, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "You don't have permission to access this workspace.", 403
            ));
        }
        
        return ResponseEntity.ok(new WorkspaceDetailDTO(workspace));
    }

    @Operation(summary = "Update Workspace", description = "Update workspace name. Only the owner can update.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Workspace updated successfully",
                content = @Content(schema = @Schema(implementation = WorkspaceResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Workspace not found — `WORKSPACE_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Workspace name already exists — `WORKSPACE_NAME_EXISTS`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not the workspace owner — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateWorkspace(
            @Parameter(description = "Workspace ID", required = true) @PathVariable String id,
            @Valid @RequestBody UpdateWorkspaceDTO body,
            @AuthenticationPrincipal User user) {
        
        Optional<Workspace> workspaceOpt = workspaceService.getWorkspaceById(id);
        
        if (workspaceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "WORKSPACE_NOT_FOUND", "Workspace not found.", 404
            ));
        }
        
        Workspace workspace = workspaceOpt.get();
        
        if (!workspaceService.isWorkspaceOwner(workspace, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "Only the workspace owner can update it.", 403
            ));
        }
        
        try {
            WorkspaceResponseDTO response = workspaceService.updateWorkspace(workspace, body);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(
                "WORKSPACE_NAME_EXISTS", e.getMessage(), 400
            ));
        }
    }

    @Operation(summary = "Delete Workspace", description = "Delete a workspace and all its boards, lists, and cards. Only the owner can delete.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Workspace deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Workspace not found — `WORKSPACE_NOT_FOUND`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Not the workspace owner — `FORBIDDEN`",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteWorkspace(
            @Parameter(description = "Workspace ID", required = true) @PathVariable String id,
            @AuthenticationPrincipal User user) {
        
        Optional<Workspace> workspaceOpt = workspaceService.getWorkspaceById(id);
        
        if (workspaceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDTO(
                "WORKSPACE_NOT_FOUND", "Workspace not found.", 404
            ));
        }
        
        Workspace workspace = workspaceOpt.get();
        
        if (!workspaceService.isWorkspaceOwner(workspace, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDTO(
                "FORBIDDEN", "Only the workspace owner can delete it.", 403
            ));
        }
        
        workspaceService.deleteWorkspace(id);
        return ResponseEntity.noContent().build();
    }
}
