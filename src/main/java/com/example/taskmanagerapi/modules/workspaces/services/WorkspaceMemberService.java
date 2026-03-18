package com.example.taskmanagerapi.modules.workspaces.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.repositories.UserRepository;
import com.example.taskmanagerapi.modules.auth.services.EmailService;
import com.example.taskmanagerapi.modules.workspaces.domain.MemberRole;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;
import com.example.taskmanagerapi.modules.workspaces.domain.WorkspaceMember;
import com.example.taskmanagerapi.modules.workspaces.dto.WorkspaceMemberDTO;
import com.example.taskmanagerapi.modules.workspaces.repositories.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberService {

    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Add the owner as OWNER member when workspace is created
     */
    @Transactional
    public void addOwner(Workspace workspace, User owner) {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(owner);
        member.setRole(MemberRole.OWNER);
        memberRepository.save(member);
    }

    /**
     * Invite a user by email or username to a workspace
     */
    @Transactional
    public WorkspaceMemberDTO inviteMember(Workspace workspace, String emailOrUsername) {
        User target = userRepository
                .findByEmailOrUsername(emailOrUsername, emailOrUsername)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (memberRepository.existsByWorkspaceAndUser(workspace, target)) {
            throw new IllegalArgumentException("USER_ALREADY_MEMBER");
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(target);
        member.setRole(MemberRole.MEMBER);
        WorkspaceMemberDTO saved = new WorkspaceMemberDTO(memberRepository.save(member));

        // Notify invited user by HTML email
        try {
            emailService.sendHtmlEmail(
                target.getEmail(),
                "You've been invited to a workspace - Task Manager",
                "member-invite",
                Map.of(
                    "inviterName", workspace.getOwner().getName(),
                    "resourceType", "workspace",
                    "resourceName", workspace.getName(),
                    "role", "member",
                    "acceptLink", frontendUrl
                )
            );
        } catch (Exception ignored) {
            // Email failure should not block the invite
        }

        return saved;
    }

    /**
     * Remove a member from a workspace
     */
    @Transactional
    public void removeMember(Workspace workspace, String userId, User requester) {
        WorkspaceMember target = memberRepository
                .findByWorkspaceAndUser(workspace, buildUserRef(userId))
                .orElseThrow(() -> new IllegalArgumentException("MEMBER_NOT_FOUND"));

        if (target.getRole() == MemberRole.OWNER) {
            throw new IllegalArgumentException("CANNOT_REMOVE_OWNER");
        }

        // Only the workspace owner or the member themselves can remove
        boolean isOwner = workspace.getOwner().getId().equals(requester.getId());
        boolean isSelf = userId.equals(requester.getId());
        if (!isOwner && !isSelf) {
            throw new SecurityException("FORBIDDEN");
        }

        memberRepository.delete(target);
    }

    /**
     * List all members of a workspace
     */
    public List<WorkspaceMemberDTO> listMembers(Workspace workspace) {
        return memberRepository.findByWorkspace(workspace)
                .stream()
                .map(WorkspaceMemberDTO::new)
                .toList();
    }

    /**
     * Check if a user is a member (or owner) of the workspace
     */
    public boolean isMember(Workspace workspace, User user) {
        return memberRepository.existsByWorkspaceAndUser(workspace, user);
    }

    private User buildUserRef(String userId) {
        User ref = new User();
        ref.setId(userId);
        return ref;
    }
}
