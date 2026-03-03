package com.example.taskmanagerapi.modules.cards.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.taskmanagerapi.modules.cards.domain.Card;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;

public interface CardRepository extends JpaRepository<Card, String> {
    List<Card> findByList(BoardList list);
    List<Card> findByListOrderByPositionAsc(BoardList list);
    void deleteByList(BoardList list);
}
