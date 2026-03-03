package com.example.taskmanagerapi.modules.lists.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;

/**
 * BoardListRepository - Data access layer for BoardList entities
 */
@Repository
public interface BoardListRepository extends JpaRepository<BoardList, String> {
    
    /**
     * Find all lists for a specific board, ordered by position
     */
    List<BoardList> findByBoardOrderByPositionAsc(Board board);
    
    /**
     * Delete all lists for a specific board
     */
    void deleteByBoard(Board board);
    
    /**
     * Find the maximum position value for lists in a board
     * Used to determine position for new lists
     */
    @Query("SELECT MAX(l.position) FROM BoardList l WHERE l.board = :board")
    Integer findMaxPositionByBoard(@Param("board") Board board);
}
