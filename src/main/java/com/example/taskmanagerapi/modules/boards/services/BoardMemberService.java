package com.example.taskmanagerapi.modules.boards.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.repositories.UserRepository;
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

    /**
     * Add the owner as OWNER member when board is created
     */
    @Transactional
    public void addOwner(Board board, User owner) {
        BoardMember member = new BoardMember();
        member.setBoard(board);
        member.setUser(owner);
        member.setRole(MemberRole.OWNER);
        boardMemberRepository.save(member);
    }

    /**
     * Invite a user (by email or username) to a specific board.
     * The target user must already be a member of the parent workspace.
     */
    @Transactional
    public BoardMemberDTO inviteMember(Board board, String emailOrUsername) {
        User target = userRepository
                .findByEmailOrUsername(emailOrUsername, emailOrUsername)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        // Target must already belong to the workspace
        boolean inWorkspace = workspaceMemberRepository
                .existsByWorkspaceAndUser(board.getWorkspace(), target);
        if (!inWorkspace) {
            throw new IllegalArgumentException("USER_NOT_IN_WORKSPACE");
        }

        if (boardMemberRepository.existsByBoardAndUser(board, target)) {
            throw new IllegalArgumentException("USER_ALREADY_MEMBER");
        }

        BoardMember member = new BoardMember();
        member.setBoard(board);
        member.setUser(target);
        member.setRole(MemberRole.MEMBER);
        return new BoardMemberDTO(boardMemberRepository.save(member));
    }

    /**
     * Remove a member from a board
     */
    @Transactional
    public void removeMember(Board board, String userId, User requester) {
        User targetRef = buildUserRef(userId);
        BoardMember target = boardMemberRepository
                .findByBoardAndUser(board, targetRef)
                .orElseThrow(() -> new IllegalArgumentException("MEMBER_NOT_FOUND"));

        if (target.getRole() == MemberRole.OWNER) {
            throw new IllegalArgumentException("CANNOT_REMOVE_OWNER");
        }

        boolean isOwner = board.getOwner().getId().equals(requester.getId());
        boolean isSelf = userId.equals(requester.getId());
        if (!isOwner && !isSelf) {
            throw new SecurityException("FORBIDDEN");
        }

        boardMemberRepository.delete(target);
    }

    /**
     * List all members of a board
     */
    public List<BoardMemberDTO> listMembers(Board board) {
        return boardMemberRepository.findByBoard(board)
                .stream()
                .map(BoardMemberDTO::new)
                .toList();
    }

    /**
     * Check if a user is a member (or owner) of the board
     */
    public boolean isMember(Board board, User user) {
        return boardMemberRepository.existsByBoardAndUser(board, user);
    }

    private User buildUserRef(String userId) {
        User ref = new User();
        ref.setId(userId);
        return ref;
    }
}
