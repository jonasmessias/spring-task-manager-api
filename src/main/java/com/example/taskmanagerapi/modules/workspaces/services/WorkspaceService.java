package com.example.taskmanagerapi.modules.workspaces.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.services.BoardService;
import com.example.taskmanagerapi.modules.storage.services.StorageService;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;
import com.example.taskmanagerapi.modules.workspaces.dto.CreateWorkspaceDTO;
import com.example.taskmanagerapi.modules.workspaces.dto.UpdateWorkspaceDTO;
import com.example.taskmanagerapi.modules.workspaces.dto.WorkspaceResponseDTO;
import com.example.taskmanagerapi.modules.workspaces.repositories.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

/**
 * WorkspaceService - Business logic for workspace operations
 * Single Responsibility: Handle business rules for workspaces
 */
@Service
@RequiredArgsConstructor
public class WorkspaceService {
    
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberService memberService;
    private final BoardService boardService;
    private final StorageService storageService;

    /**
     * Create a new workspace for a user
     */
    @Transactional
    public WorkspaceResponseDTO createWorkspace(@NonNull CreateWorkspaceDTO dto, @NonNull User owner) {
        // Check if name already exists for this user
        if (workspaceRepository.existsByOwnerAndName(owner, dto.name())) {
            throw new IllegalArgumentException("Workspace with name '" + dto.name() + "' already exists");
        }
        
        Workspace workspace = new Workspace();
        workspace.setName(dto.name());
        workspace.setOwner(owner);
        
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // Register owner as OWNER member
        memberService.addOwner(savedWorkspace, owner);

        return new WorkspaceResponseDTO(savedWorkspace);
    }

    /**
     * Get all workspaces for a user (owned + invited), ordered by creation date
     */
    public List<WorkspaceResponseDTO> getWorkspacesByUser(@NonNull User user) {
        return workspaceRepository.findAllByMemberUser(user)
                .stream()
                .map(WorkspaceResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Find a workspace by ID
     */
    public Optional<Workspace> getWorkspaceById(@NonNull String id) {
        return workspaceRepository.findById(id);
    }

    /**
     * Save a workspace entity (used for field updates like cover)
     */
    public Workspace saveWorkspace(@NonNull Workspace workspace) {
        workspace.setUpdatedAt(LocalDateTime.now());
        return workspaceRepository.save(workspace);
    }

    /**
     * Update an existing workspace
     */
    @Transactional
    public WorkspaceResponseDTO updateWorkspace(@NonNull Workspace workspace, @NonNull UpdateWorkspaceDTO dto) {
        if (dto.name() != null && !dto.name().isBlank()) {
            // Check if new name conflicts with existing workspace
            if (!workspace.getName().equals(dto.name()) && 
                workspaceRepository.existsByOwnerAndName(workspace.getOwner(), dto.name())) {
                throw new IllegalArgumentException("Workspace with name '" + dto.name() + "' already exists");
            }
            workspace.setName(dto.name());
        }
        
        workspace.setUpdatedAt(LocalDateTime.now());
        Workspace updatedWorkspace = workspaceRepository.save(workspace);
        return new WorkspaceResponseDTO(updatedWorkspace);
    }

    /**
     * Delete a workspace by ID
     * Cleans up S3 files (board covers, card attachments) before cascading deletion
     */
    @Transactional
    public void deleteWorkspace(@NonNull String id) {
        Workspace workspace = workspaceRepository.findById(
                Objects.requireNonNull(id, "Workspace ID cannot be null")
            )
            .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        // Delete workspace cover from S3
        if (workspace.getCoverUrl() != null) {
            storageService.deleteFile(workspace.getCoverUrl());
        }

        // Clean up S3 files for each board (covers + list/card attachments)
        if (workspace.getBoards() != null) {
            for (Board board : workspace.getBoards()) {
                boardService.deleteBoard(board.getId());
            }
        }

        workspaceRepository.delete(
            Objects.requireNonNull(workspace, "Workspace cannot be null")
        );
    }

    /**
     * Check if a workspace belongs to a user
     */
    public boolean isWorkspaceOwner(@NonNull Workspace workspace, @NonNull User user) {
        return workspace.getOwner().getId().equals(user.getId());
    }
}
