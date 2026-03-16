package com.example.taskmanagerapi.modules.boards.controllers;

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
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.dto.BoardMemberDTO;
import com.example.taskmanagerapi.modules.boards.services.BoardMemberService;
import com.example.taskmanagerapi.modules.boards.services.BoardService;
import com.example.taskmanagerapi.modules.workspaces.dto.InviteMemberDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/boards/{boardId}/members")
@RequiredArgsConstructor
@Tag(name = "Board Members", description = "Manage members of a specific board")
@SecurityRequirement(name = "Bearer Authentication")
public class BoardMemberController {

    private final BoardService boardService;
    private final BoardMemberService memberService;

    @Operation(summary = "List members", description = "List all members of a board")
    @GetMapping
    public ResponseEntity<Object> listMembers(
            @PathVariable String boardId,
            @AuthenticationPrincipal User user) {

        Board board = resolveBoard(boardId);
        if (board == null) return notFound("BOARD_NOT_FOUND", "Board not found");

        if (!memberService.isMember(board, user)) {
            return forbidden("FORBIDDEN", "You are not a member of this board");
        }

        List<BoardMemberDTO> members = memberService.listMembers(board);
        return ResponseEntity.ok(members);
    }

    @Operation(summary = "Invite member", description = "Invite a workspace member to a specific board by email or username")
    @PostMapping
    public ResponseEntity<Object> inviteMember(
            @PathVariable String boardId,
            @Valid @RequestBody InviteMemberDTO body,
            @AuthenticationPrincipal User user) {

        Board board = resolveBoard(boardId);
        if (board == null) return notFound("BOARD_NOT_FOUND", "Board not found");

        if (!board.getOwner().getId().equals(user.getId())) {
            return forbidden("FORBIDDEN", "Only the board owner can invite members");
        }

        try {
            BoardMemberDTO member = memberService.inviteMember(board, body.emailOrUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(member);
        } catch (IllegalArgumentException e) {
            return switch (e.getMessage()) {
                case "USER_NOT_FOUND" -> notFound("USER_NOT_FOUND", "No user found with that email or username");
                case "USER_NOT_IN_WORKSPACE" -> badRequest("USER_NOT_IN_WORKSPACE", "User must be a workspace member first");
                case "USER_ALREADY_MEMBER" -> badRequest("USER_ALREADY_MEMBER", "User is already a member of this board");
                default -> badRequest("BAD_REQUEST", e.getMessage());
            };
        }
    }

    @Operation(summary = "Remove member", description = "Remove a member from the board")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Object> removeMember(
            @PathVariable String boardId,
            @PathVariable String userId,
            @AuthenticationPrincipal User user) {

        Board board = resolveBoard(boardId);
        if (board == null) return notFound("BOARD_NOT_FOUND", "Board not found");

        try {
            memberService.removeMember(board, userId, user);
            return ResponseEntity.ok(new MessageResponseDTO("Member removed successfully"));
        } catch (IllegalArgumentException e) {
            return switch (e.getMessage()) {
                case "MEMBER_NOT_FOUND" -> notFound("MEMBER_NOT_FOUND", "Member not found in this board");
                case "CANNOT_REMOVE_OWNER" -> badRequest("CANNOT_REMOVE_OWNER", "The board owner cannot be removed");
                default -> badRequest("BAD_REQUEST", e.getMessage());
            };
        } catch (SecurityException e) {
            return forbidden("FORBIDDEN", "You don't have permission to remove this member");
        }
    }

    // -------------------------------------------------------------------------

    private Board resolveBoard(String boardId) {
        return boardService.getBoardById(boardId).orElse(null);
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
