package com.example.taskmanagerapi.modules.workspaces.dto;

import java.time.LocalDateTime;

import com.example.taskmanagerapi.modules.workspaces.domain.MemberRole;
import com.example.taskmanagerapi.modules.workspaces.domain.WorkspaceMember;

public record WorkspaceMemberDTO(
    String userId,
    String name,
    String username,
    String email,
    MemberRole role,
    LocalDateTime joinedAt
) {
    public WorkspaceMemberDTO(WorkspaceMember member) {
        this(
            member.getUser().getId(),
            member.getUser().getName(),
            member.getUser().getUsername(),
            member.getUser().getEmail(),
            member.getRole(),
            member.getJoinedAt()
        );
    }
}
