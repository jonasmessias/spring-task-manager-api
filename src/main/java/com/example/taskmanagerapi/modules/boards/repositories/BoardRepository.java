package com.example.taskmanagerapi.modules.boards.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.domain.BoardType;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;

public interface BoardRepository extends JpaRepository<Board, String> {
    List<Board> findByWorkspaceOrderByCreatedAtDesc(Workspace workspace);
    List<Board> findByWorkspaceAndType(Workspace workspace, BoardType type);
    long countByWorkspace(Workspace workspace);
}
