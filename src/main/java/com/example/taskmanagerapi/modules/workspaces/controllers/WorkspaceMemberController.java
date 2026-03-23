package com.example.taskmanagerapi.modules.workspaces.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.dto.MessageResponseDTO;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;
import com.example.taskmanagerapi.modules.workspaces.dto.InviteMemberDTO;
import com.example.taskmanagerapi.modules.workspaces.dto.WorkspaceMemberDTO;
import com.example.taskmanagerapi.modules.workspaces.services.WorkspaceMemberService;
import com.example.taskmanagerapi.modules.workspaces.services.WorkspaceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/workspaces/{workspaceId}/members")
@RequiredArgsConstructor
@Tag(name = "Workspace Members", description = "Manage members of a workspace")
@SecurityRequirement(name = "Bearer Authentication")
public class WorkspaceMemberController {

    private final WorkspaceService workspaceService;
    private final WorkspaceMemberService memberService;

    @Operation(summary = "List members", description = "List all members of a workspace")
    @GetMapping
    public ResponseEntity<List<WorkspaceMemberDTO>> listMembers(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User user) {

        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        workspaceService.requireMember(workspace, user);

        return ResponseEntity.ok(memberService.listMembers(workspace));
    }

    @Operation(summary = "Invite member", description = "Invite a user by email or username to the workspace")
    @PostMapping
    public ResponseEntity<WorkspaceMemberDTO> inviteMember(
            @PathVariable String workspaceId,
            @Valid @RequestBody InviteMemberDTO body,
            @AuthenticationPrincipal User user) {

        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        workspaceService.requireOwner(workspace, user);

        WorkspaceMemberDTO member = memberService.inviteMember(workspace, body.emailOrUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @Operation(summary = "Remove member", description = "Remove a member from the workspace")
    @DeleteMapping("/{userId}")
    public ResponseEntity<MessageResponseDTO> removeMember(
            @PathVariable String workspaceId,
            @PathVariable String userId,
            @AuthenticationPrincipal User user) {

        Workspace workspace = workspaceService.getWorkspaceById(workspaceId);
        memberService.removeMember(workspace, userId, user);
        return ResponseEntity.ok(new MessageResponseDTO("Member removed successfully"));
    }
}
