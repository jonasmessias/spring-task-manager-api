package com.example.taskmanagerapi.modules.workspaces.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.infra.exception.ConflictException;
import com.example.taskmanagerapi.infra.exception.ForbiddenException;
import com.example.taskmanagerapi.infra.exception.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
public class WorkspaceService {
    
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberService memberService;
    private final BoardService boardService;
    private final StorageService storageService;

    @Transactional
    public WorkspaceResponseDTO createWorkspace(@NonNull CreateWorkspaceDTO dto, @NonNull User owner) {
        if (workspaceRepository.existsByOwnerAndName(owner, dto.name())) {
            throw new ConflictException("WORKSPACE_NAME_EXISTS",
                    "Workspace with name '" + dto.name() + "' already exists");
        }
        
        Workspace workspace = new Workspace();
        workspace.setName(dto.name());
        workspace.setOwner(owner);
        
        Workspace savedWorkspace = workspaceRepository.save(workspace);
        memberService.addOwner(savedWorkspace, owner);
        return new WorkspaceResponseDTO(savedWorkspace);
    }

    public List<WorkspaceResponseDTO> getWorkspacesByUser(@NonNull User user) {
        return workspaceRepository.findAllByMemberUser(user)
                .stream()
                .map(WorkspaceResponseDTO::new)
                .collect(Collectors.toList());
    }

    public Workspace getWorkspaceById(@NonNull String id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WORKSPACE_NOT_FOUND",
                        "Workspace not found."));
    }

    public void requireMember(@NonNull Workspace workspace, @NonNull User user) {
        if (!memberService.isMember(workspace, user)) {
            throw new ForbiddenException("FORBIDDEN",
                    "You don't have permission to access this workspace.");
        }
    }

    public void requireOwner(@NonNull Workspace workspace, @NonNull User user) {
        if (!isWorkspaceOwner(workspace, user)) {
            throw new ForbiddenException("FORBIDDEN",
                    "Only the workspace owner can perform this action.");
        }
    }

    public Workspace saveWorkspace(@NonNull Workspace workspace) {
        workspace.setUpdatedAt(LocalDateTime.now());
        return workspaceRepository.save(workspace);
    }

    @Transactional
    public WorkspaceResponseDTO updateWorkspace(@NonNull Workspace workspace, @NonNull UpdateWorkspaceDTO dto) {
        if (dto.name() != null && !dto.name().isBlank()) {
            if (!workspace.getName().equals(dto.name()) && 
                workspaceRepository.existsByOwnerAndName(workspace.getOwner(), dto.name())) {
                throw new ConflictException("WORKSPACE_NAME_EXISTS",
                        "Workspace with name '" + dto.name() + "' already exists");
            }
            workspace.setName(dto.name());
        }
        
        workspace.setUpdatedAt(LocalDateTime.now());
        Workspace updatedWorkspace = workspaceRepository.save(workspace);
        return new WorkspaceResponseDTO(updatedWorkspace);
    }

    @Transactional
    public void deleteWorkspace(@NonNull String id) {
        Workspace workspace = workspaceRepository.findById(
                Objects.requireNonNull(id, "Workspace ID cannot be null")
            )
            .orElseThrow(() -> new ResourceNotFoundException("WORKSPACE_NOT_FOUND", "Workspace not found."));

        // Delete workspace cover from S3
        if (workspace.getCoverUrl() != null) {
            storageService.deleteFile(workspace.getCoverUrl());
        }

        if (workspace.getBoards() != null) {
            for (Board board : workspace.getBoards()) {
                boardService.deleteBoard(board.getId());
            }
        }

        workspaceRepository.delete(
            Objects.requireNonNull(workspace, "Workspace cannot be null")
        );
    }

    public boolean isWorkspaceOwner(@NonNull Workspace workspace, @NonNull User user) {
        return workspace.getOwner().getId().equals(user.getId());
    }
}
