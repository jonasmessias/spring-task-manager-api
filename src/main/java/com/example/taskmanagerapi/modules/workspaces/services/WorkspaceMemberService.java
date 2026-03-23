package com.example.taskmanagerapi.modules.workspaces.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.infra.exception.ConflictException;
import com.example.taskmanagerapi.infra.exception.ForbiddenException;
import com.example.taskmanagerapi.infra.exception.ResourceNotFoundException;
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

    @Transactional
    public void addOwner(Workspace workspace, User owner) {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(owner);
        member.setRole(MemberRole.OWNER);
        memberRepository.save(member);
    }

    @Transactional
    public WorkspaceMemberDTO inviteMember(Workspace workspace, String emailOrUsername) {
        User target = userRepository
                .findByEmailOrUsername(emailOrUsername, emailOrUsername)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "No user found with that email or username."));

        if (memberRepository.existsByWorkspaceAndUser(workspace, target)) {
            throw new ConflictException("USER_ALREADY_MEMBER",
                    "User is already a member of this workspace.");
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(target);
        member.setRole(MemberRole.MEMBER);
        WorkspaceMemberDTO saved = new WorkspaceMemberDTO(memberRepository.save(member));

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
        }

        return saved;
    }

    @Transactional
    public void removeMember(Workspace workspace, String userId, User requester) {
        WorkspaceMember target = memberRepository
                .findByWorkspaceAndUser(workspace, buildUserRef(userId))
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND",
                        "Member not found in this workspace."));

        if (target.getRole() == MemberRole.OWNER) {
            throw new ConflictException("CANNOT_REMOVE_OWNER",
                    "The workspace owner cannot be removed.");
        }

        boolean isOwner = workspace.getOwner().getId().equals(requester.getId());
        boolean isSelf = userId.equals(requester.getId());
        if (!isOwner && !isSelf) {
            throw new ForbiddenException("FORBIDDEN",
                    "You don't have permission to remove this member.");
        }

        memberRepository.delete(target);
    }

    public List<WorkspaceMemberDTO> listMembers(Workspace workspace) {
        return memberRepository.findByWorkspace(workspace)
                .stream()
                .map(WorkspaceMemberDTO::new)
                .toList();
    }

    public boolean isMember(Workspace workspace, User user) {
        return memberRepository.existsByWorkspaceAndUser(workspace, user);
    }

    private User buildUserRef(String userId) {
        User ref = new User();
        ref.setId(userId);
        return ref;
    }
}
