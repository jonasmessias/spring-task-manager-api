package com.example.taskmanagerapi.modules.lists.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.taskmanagerapi.modules.boards.domain.Board;
import com.example.taskmanagerapi.modules.cards.domain.Card;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * BoardList entity - Container for cards within a board
 * Independent module for better separation of concerns
 */
@Entity
@Table(name = "board_lists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardList {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "list_position")
    private Integer position;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
    
    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Card> cards = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper method to add card
    public void addCard(Card card) {
        cards.add(card);
        card.setList(this);
    }
    
    // Helper method to remove card
    public void removeCard(Card card) {
        cards.remove(card);
        card.setList(null);
    }
}
