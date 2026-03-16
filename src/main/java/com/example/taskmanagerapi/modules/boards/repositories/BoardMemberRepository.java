package com.example.taskmanagerapi.modules.boards.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.boards.domain.BoardMember;

@Repository
public interface BoardMemberRepository extends JpaRepository<BoardMember, String> {

    List<BoardMember> findByBoard(Board board);

    Optional<BoardMember> findByBoardAndUser(Board board, User user);

    boolean existsByBoardAndUser(Board board, User user);

    void deleteByBoardAndUser(Board board, User user);
}
