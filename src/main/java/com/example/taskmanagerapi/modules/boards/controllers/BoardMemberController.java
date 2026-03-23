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
    public ResponseEntity<List<BoardMemberDTO>> listMembers(
            @PathVariable String boardId,
            @AuthenticationPrincipal User user) {

        Board board = boardService.requireBoardById(boardId);
        boardService.requireMember(board, user);

        return ResponseEntity.ok(memberService.listMembers(board));
    }

    @Operation(summary = "Invite member", description = "Invite a workspace member to a specific board by email or username")
    @PostMapping
    public ResponseEntity<BoardMemberDTO> inviteMember(
            @PathVariable String boardId,
            @Valid @RequestBody InviteMemberDTO body,
            @AuthenticationPrincipal User user) {

        Board board = boardService.requireBoardById(boardId);
        boardService.requireOwner(board, user);

        BoardMemberDTO member = memberService.inviteMember(board, body.emailOrUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @Operation(summary = "Remove member", description = "Remove a member from the board")
    @DeleteMapping("/{userId}")
    public ResponseEntity<MessageResponseDTO> removeMember(
            @PathVariable String boardId,
            @PathVariable String userId,
            @AuthenticationPrincipal User user) {

        Board board = boardService.requireBoardById(boardId);
        memberService.removeMember(board, userId, user);
        return ResponseEntity.ok(new MessageResponseDTO("Member removed successfully"));
    }
}
