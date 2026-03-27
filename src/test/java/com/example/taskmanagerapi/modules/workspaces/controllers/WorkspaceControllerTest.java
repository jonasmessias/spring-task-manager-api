package com.example.taskmanagerapi.modules.workspaces.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.taskmanagerapi.infra.exception.ForbiddenException;
import com.example.taskmanagerapi.infra.exception.ResourceNotFoundException;
import com.example.taskmanagerapi.infra.security.RateLimitFilter;
import com.example.taskmanagerapi.infra.security.SecurityFilter;
import com.example.taskmanagerapi.infra.security.TokenService;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.storage.services.StorageService;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;
import com.example.taskmanagerapi.modules.workspaces.dto.WorkspaceResponseDTO;
import com.example.taskmanagerapi.modules.workspaces.services.WorkspaceService;

@WebMvcTest(WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("WorkspaceController")
class WorkspaceControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private WorkspaceService workspaceService;
    @MockitoBean private StorageService storageService;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private SecurityFilter securityFilter;
    @MockitoBean private RateLimitFilter rateLimitFilter;

    private User testUser;
    private Workspace testWorkspace;
    private WorkspaceResponseDTO testResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setEmail("john@test.com");
        testUser.setName("John Doe");

        testWorkspace = new Workspace();
        testWorkspace.setId("ws-1");
        testWorkspace.setName("My Workspace");
        testWorkspace.setOwner(testUser);
        testWorkspace.setBoards(new ArrayList<>());
        testWorkspace.setCreatedAt(LocalDateTime.now());

        testResponse = new WorkspaceResponseDTO(testWorkspace);

        var auth = new UsernamePasswordAuthenticationToken(
                testUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("POST /workspaces")
    class CreateWorkspace {

        @Test
        @DisplayName("should return 201 on successful creation")
        void shouldReturn201() throws Exception {
            when(workspaceService.createWorkspace(any(), any())).thenReturn(testResponse);

            mockMvc.perform(post("/workspaces")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "My Workspace"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("ws-1"))
                    .andExpect(jsonPath("$.name").value("My Workspace"));
        }
    }

    @Nested
    @DisplayName("GET /workspaces")
    class GetAllWorkspaces {

        @Test
        @DisplayName("should return 200 with workspace list")
        void shouldReturn200() throws Exception {
            when(workspaceService.getWorkspacesByUser(any())).thenReturn(List.of(testResponse));

            mockMvc.perform(get("/workspaces"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("ws-1"));
        }
    }

    @Nested
    @DisplayName("PUT /workspaces/{id}")
    class UpdateWorkspace {

        @Test
        @DisplayName("should return 200 on successful update")
        void shouldReturn200() throws Exception {
            when(workspaceService.getWorkspaceById("ws-1")).thenReturn(testWorkspace);
            doNothing().when(workspaceService).requireOwner(testWorkspace, testUser);
            when(workspaceService.updateWorkspace(any(), any())).thenReturn(testResponse);

            mockMvc.perform(put("/workspaces/ws-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Updated Workspace"}
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 404 when workspace not found")
        void shouldReturn404() throws Exception {
            when(workspaceService.getWorkspaceById("unknown"))
                    .thenThrow(new ResourceNotFoundException("WORKSPACE_NOT_FOUND", "Workspace not found."));

            mockMvc.perform(put("/workspaces/unknown")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Updated Workspace"}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /workspaces/{id}")
    class DeleteWorkspace {

        @Test
        @DisplayName("should return 204 on successful deletion")
        void shouldReturn204() throws Exception {
            when(workspaceService.getWorkspaceById("ws-1")).thenReturn(testWorkspace);
            doNothing().when(workspaceService).requireOwner(testWorkspace, testUser);
            doNothing().when(workspaceService).deleteWorkspace("ws-1");

            mockMvc.perform(delete("/workspaces/ws-1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when not owner")
        void shouldReturn403() throws Exception {
            when(workspaceService.getWorkspaceById("ws-1")).thenReturn(testWorkspace);
            org.mockito.Mockito.doThrow(new ForbiddenException("FORBIDDEN", "Only the workspace owner can perform this action."))
                    .when(workspaceService).requireOwner(any(), any());

            mockMvc.perform(delete("/workspaces/ws-1"))
                    .andExpect(status().isForbidden());
        }
    }
}
