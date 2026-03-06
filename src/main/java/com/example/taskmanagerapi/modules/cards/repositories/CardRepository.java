package com.example.taskmanagerapi.modules.cards.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.taskmanagerapi.modules.cards.domain.Card;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;

public interface CardRepository extends JpaRepository<Card, String> {
    List<Card> findByList(BoardList list);
    List<Card> findByListOrderByPositionAsc(BoardList list);
    void deleteByList(BoardList list);

    /**
     * Find the maximum position value for cards in a list.
     * Used to auto-append new cards at the end.
     */
    @Query("SELECT MAX(c.position) FROM Card c WHERE c.list = :list")
    Integer findMaxPositionByList(@Param("list") BoardList list);
}
