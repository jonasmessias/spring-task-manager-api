package com.example.taskmanagerapi.modules.workspaces.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.taskmanagerapi.infra.exception.ConflictException;
import com.example.taskmanagerapi.infra.exception.ForbiddenException;
import com.example.taskmanagerapi.infra.exception.ResourceNotFoundException;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.boards.services.BoardService;
import com.example.taskmanagerapi.modules.storage.services.StorageService;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;
import com.example.taskmanagerapi.modules.workspaces.dto.CreateWorkspaceDTO;
import com.example.taskmanagerapi.modules.workspaces.dto.UpdateWorkspaceDTO;
import com.example.taskmanagerapi.modules.workspaces.dto.WorkspaceResponseDTO;
import com.example.taskmanagerapi.modules.workspaces.repositories.WorkspaceRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceService")
class WorkspaceServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberService memberService;
    @Mock private BoardService boardService;
    @Mock private StorageService storageService;

    @InjectMocks
    private WorkspaceService workspaceService;

    private User owner;
    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId("user-1");
        owner.setName("John Doe");
        owner.setEmail("john@test.com");

        testWorkspace = new Workspace();
        testWorkspace.setId("ws-1");
        testWorkspace.setName("My Workspace");
        testWorkspace.setOwner(owner);
        testWorkspace.setBoards(new ArrayList<>());
        testWorkspace.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createWorkspace")
    class CreateWorkspace {

        @Test
        @DisplayName("should create workspace successfully")
        void shouldCreateWorkspace() {
            CreateWorkspaceDTO dto = new CreateWorkspaceDTO("My Workspace");

            when(workspaceRepository.existsByOwnerAndName(owner, "My Workspace")).thenReturn(false);
            when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);

            WorkspaceResponseDTO result = workspaceService.createWorkspace(dto, owner);

            assertThat(result.id()).isEqualTo("ws-1");
            assertThat(result.name()).isEqualTo("My Workspace");
            verify(memberService).addOwner(testWorkspace, owner);
        }

        @Test
        @DisplayName("should throw ConflictException when workspace name already exists")
        void shouldThrowWhenNameExists() {
            CreateWorkspaceDTO dto = new CreateWorkspaceDTO("My Workspace");

            when(workspaceRepository.existsByOwnerAndName(owner, "My Workspace")).thenReturn(true);

            assertThatThrownBy(() -> workspaceService.createWorkspace(dto, owner))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    @DisplayName("getWorkspaceById")
    class GetWorkspaceById {

        @Test
        @DisplayName("should return workspace")
        void shouldReturnWorkspace() {
            when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(testWorkspace));

            Workspace result = workspaceService.getWorkspaceById("ws-1");

            assertThat(result.getId()).isEqualTo("ws-1");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(workspaceRepository.findById("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workspaceService.getWorkspaceById("unknown"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("requireMember")
    class RequireMember {

        @Test
        @DisplayName("should pass when user is member")
        void shouldPassWhenMember() {
            when(memberService.isMember(testWorkspace, owner)).thenReturn(true);

            workspaceService.requireMember(testWorkspace, owner);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user is not member")
        void shouldThrowWhenNotMember() {
            when(memberService.isMember(testWorkspace, owner)).thenReturn(false);

            assertThatThrownBy(() -> workspaceService.requireMember(testWorkspace, owner))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("requireOwner")
    class RequireOwner {

        @Test
        @DisplayName("should pass when user is owner")
        void shouldPassWhenOwner() {
            workspaceService.requireOwner(testWorkspace, owner);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user is not owner")
        void shouldThrowWhenNotOwner() {
            User otherUser = new User();
            otherUser.setId("user-2");

            assertThatThrownBy(() -> workspaceService.requireOwner(testWorkspace, otherUser))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("updateWorkspace")
    class UpdateWorkspace {

        @Test
        @DisplayName("should update workspace name")
        void shouldUpdateName() {
            UpdateWorkspaceDTO dto = new UpdateWorkspaceDTO("New Name");

            when(workspaceRepository.existsByOwnerAndName(owner, "New Name")).thenReturn(false);
            when(workspaceRepository.save(any(Workspace.class))).thenReturn(testWorkspace);

            WorkspaceResponseDTO result = workspaceService.updateWorkspace(testWorkspace, dto);

            assertThat(result).isNotNull();
            verify(workspaceRepository).save(any(Workspace.class));
        }

        @Test
        @DisplayName("should throw ConflictException when new name already exists")
        void shouldThrowWhenNewNameExists() {
            UpdateWorkspaceDTO dto = new UpdateWorkspaceDTO("Existing Name");

            when(workspaceRepository.existsByOwnerAndName(owner, "Existing Name")).thenReturn(true);

            assertThatThrownBy(() -> workspaceService.updateWorkspace(testWorkspace, dto))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    @DisplayName("deleteWorkspace")
    class DeleteWorkspace {

        @Test
        @DisplayName("should delete workspace without cover")
        void shouldDeleteWithoutCover() {
            when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(testWorkspace));

            workspaceService.deleteWorkspace("ws-1");

            verify(workspaceRepository).delete(testWorkspace);
        }

        @Test
        @DisplayName("should delete workspace cover from S3")
        void shouldDeleteCover() {
            testWorkspace.setCoverUrl("https://s3.amazonaws.com/covers/ws-1.jpg");
            when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(testWorkspace));

            workspaceService.deleteWorkspace("ws-1");

            verify(storageService).deleteFile("https://s3.amazonaws.com/covers/ws-1.jpg");
            verify(workspaceRepository).delete(testWorkspace);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when workspace not found")
        void shouldThrowWhenNotFound() {
            when(workspaceRepository.findById("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workspaceService.deleteWorkspace("unknown"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("isWorkspaceOwner")
    class IsWorkspaceOwner {

        @Test
        @DisplayName("should return true for owner")
        void shouldReturnTrueForOwner() {
            assertThat(workspaceService.isWorkspaceOwner(testWorkspace, owner)).isTrue();
        }

        @Test
        @DisplayName("should return false for non-owner")
        void shouldReturnFalseForNonOwner() {
            User other = new User();
            other.setId("user-other");

            assertThat(workspaceService.isWorkspaceOwner(testWorkspace, other)).isFalse();
        }
    }
}
