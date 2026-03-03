package com.example.taskmanagerapi.modules.boards.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.domain.BoardType;

public interface BoardRepository extends JpaRepository<Board, String> {
    List<Board> findByOwner(User owner);
    List<Board> findByOwnerOrderByCreatedAtDesc(User owner);
    List<Board> findByOwnerAndType(User owner, BoardType type);
}
