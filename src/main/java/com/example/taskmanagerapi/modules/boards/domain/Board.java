package com.example.taskmanagerapi.modules.boards.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.lists.domain.BoardList;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Board entity - Container within a workspace
 * Scalable design to support multiple board types (BOARD, KANBAN, CALENDAR, etc.)
 * Default type is BOARD
 * Now belongs to a Workspace for better organization
 */
@Entity
@Table(name = "boards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Board {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoardType type;
    
    @Column(length = 500)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;
    
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardList> lists = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper methods to maintain bidirectional relationship
    public void addList(BoardList list) {
        lists.add(list);
        list.setBoard(this);
    }
    
    public void removeList(BoardList list) {
        lists.remove(list);
        list.setBoard(null);
    }
}
