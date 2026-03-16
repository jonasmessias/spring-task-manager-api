package com.example.taskmanagerapi.modules.boards.dto;

import java.time.LocalDateTime;

import com.example.taskmanagerapi.modules.boards.domain.BoardMember;
import com.example.taskmanagerapi.modules.workspaces.domain.MemberRole;

public record BoardMemberDTO(
    String userId,
    String name,
    String username,
    String email,
    MemberRole role,
    LocalDateTime joinedAt
) {
    public BoardMemberDTO(BoardMember member) {
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
