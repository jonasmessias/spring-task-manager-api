package com.example.taskmanagerapi.modules.cards.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.example.taskmanagerapi.modules.cards.domain.Attachment;
import com.example.taskmanagerapi.modules.cards.domain.Card;

public interface AttachmentRepository extends JpaRepository<Attachment, String> {

    List<Attachment> findByCardOrderByCreatedAtDesc(Card card);

    long countByCard(Card card);

    @Modifying
    void deleteByCard(Card card);
}
