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
import com.example.taskmanagerapi.modules.auth.dto.ErrorResponseDTO;
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
    public ResponseEntity<Object> listMembers(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User user) {

        Workspace workspace = resolveWorkspace(workspaceId);
        if (workspace == null) return notFound("WORKSPACE_NOT_FOUND", "Workspace not found");

        if (!memberService.isMember(workspace, user)) {
            return forbidden("FORBIDDEN", "You are not a member of this workspace");
        }

        List<WorkspaceMemberDTO> members = memberService.listMembers(workspace);
        return ResponseEntity.ok(members);
    }

    @Operation(summary = "Invite member", description = "Invite a user by email or username to the workspace")
    @PostMapping
    public ResponseEntity<Object> inviteMember(
            @PathVariable String workspaceId,
            @Valid @RequestBody InviteMemberDTO body,
            @AuthenticationPrincipal User user) {

        Workspace workspace = resolveWorkspace(workspaceId);
        if (workspace == null) return notFound("WORKSPACE_NOT_FOUND", "Workspace not found");

        if (!workspaceService.isWorkspaceOwner(workspace, user)) {
            return forbidden("FORBIDDEN", "Only the workspace owner can invite members");
        }

        try {
            WorkspaceMemberDTO member = memberService.inviteMember(workspace, body.emailOrUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(member);
        } catch (IllegalArgumentException e) {
            return switch (e.getMessage()) {
                case "USER_NOT_FOUND" -> notFound("USER_NOT_FOUND", "No user found with that email or username");
                case "USER_ALREADY_MEMBER" -> badRequest("USER_ALREADY_MEMBER", "User is already a member of this workspace");
                default -> badRequest("BAD_REQUEST", e.getMessage());
            };
        }
    }

    @Operation(summary = "Remove member", description = "Remove a member from the workspace")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Object> removeMember(
            @PathVariable String workspaceId,
            @PathVariable String userId,
            @AuthenticationPrincipal User user) {

        Workspace workspace = resolveWorkspace(workspaceId);
        if (workspace == null) return notFound("WORKSPACE_NOT_FOUND", "Workspace not found");

        try {
            memberService.removeMember(workspace, userId, user);
            return ResponseEntity.ok(new MessageResponseDTO("Member removed successfully"));
        } catch (IllegalArgumentException e) {
            return switch (e.getMessage()) {
                case "MEMBER_NOT_FOUND" -> notFound("MEMBER_NOT_FOUND", "Member not found in this workspace");
                case "CANNOT_REMOVE_OWNER" -> badRequest("CANNOT_REMOVE_OWNER", "The workspace owner cannot be removed");
                default -> badRequest("BAD_REQUEST", e.getMessage());
            };
        } catch (SecurityException e) {
            return forbidden("FORBIDDEN", "You don't have permission to remove this member");
        }
    }

    // -------------------------------------------------------------------------

    private Workspace resolveWorkspace(String workspaceId) {
        return workspaceService.getWorkspaceById(workspaceId).orElse(null);
    }

    private ResponseEntity<Object> notFound(String code, String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO(code, message, 404));
    }

    private ResponseEntity<Object> forbidden(String code, String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDTO(code, message, 403));
    }

    private ResponseEntity<Object> badRequest(String code, String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(code, message, 400));
    }
}
