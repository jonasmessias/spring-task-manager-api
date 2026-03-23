package com.example.taskmanagerapi.modules.boards.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.infra.exception.BusinessException;
import com.example.taskmanagerapi.infra.exception.ConflictException;
import com.example.taskmanagerapi.infra.exception.ForbiddenException;
import com.example.taskmanagerapi.infra.exception.ResourceNotFoundException;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.repositories.UserRepository;
import com.example.taskmanagerapi.modules.auth.services.EmailService;
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.domain.BoardMember;
import com.example.taskmanagerapi.modules.boards.dto.BoardMemberDTO;
import com.example.taskmanagerapi.modules.boards.repositories.BoardMemberRepository;
import com.example.taskmanagerapi.modules.workspaces.domain.MemberRole;
import com.example.taskmanagerapi.modules.workspaces.repositories.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardMemberService {

    private final BoardMemberRepository boardMemberRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Transactional
    public void addOwner(Board board, User owner) {
        BoardMember member = new BoardMember();
        member.setBoard(board);
        member.setUser(owner);
        member.setRole(MemberRole.OWNER);
        boardMemberRepository.save(member);
    }

    @Transactional
    public BoardMemberDTO inviteMember(Board board, String emailOrUsername) {
        User target = userRepository
                .findByEmailOrUsername(emailOrUsername, emailOrUsername)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "No user found with that email or username."));

        boolean inWorkspace = workspaceMemberRepository
                .existsByWorkspaceAndUser(board.getWorkspace(), target);
        if (!inWorkspace) {
            throw new BusinessException("USER_NOT_IN_WORKSPACE",
                    "User must be a workspace member first.");
        }

        if (boardMemberRepository.existsByBoardAndUser(board, target)) {
            throw new ConflictException("USER_ALREADY_MEMBER",
                    "User is already a member of this board.");
        }

        BoardMember member = new BoardMember();
        member.setBoard(board);
        member.setUser(target);
        member.setRole(MemberRole.MEMBER);
        BoardMemberDTO saved = new BoardMemberDTO(boardMemberRepository.save(member));

        try {
            emailService.sendHtmlEmail(
                target.getEmail(),
                "You've been invited to a board - Task Manager",
                "member-invite",
                Map.of(
                    "inviterName", board.getOwner().getName(),
                    "resourceType", "board",
                    "resourceName", board.getName(),
                    "role", "member",
                    "acceptLink", frontendUrl
                )
            );
        } catch (Exception ignored) {
        }

        return saved;
    }

    @Transactional
    public void removeMember(Board board, String userId, User requester) {
        User targetRef = buildUserRef(userId);
        BoardMember target = boardMemberRepository
                .findByBoardAndUser(board, targetRef)
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND",
                        "Member not found in this board."));

        if (target.getRole() == MemberRole.OWNER) {
            throw new ConflictException("CANNOT_REMOVE_OWNER",
                    "The board owner cannot be removed.");
        }

        boolean isOwner = board.getOwner().getId().equals(requester.getId());
        boolean isSelf = userId.equals(requester.getId());
        if (!isOwner && !isSelf) {
            throw new ForbiddenException("FORBIDDEN",
                    "You don't have permission to remove this member.");
        }

        boardMemberRepository.delete(target);
    }

    public List<BoardMemberDTO> listMembers(Board board) {
        return boardMemberRepository.findByBoard(board)
                .stream()
                .map(BoardMemberDTO::new)
                .toList();
    }

    public boolean isMember(Board board, User user) {
        return boardMemberRepository.existsByBoardAndUser(board, user);
    }

    private User buildUserRef(String userId) {
        User ref = new User();
        ref.setId(userId);
        return ref;
    }
}
