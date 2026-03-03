package com.example.taskmanagerapi.modules.workspaces.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;

/**
 * Repository for Workspace entity
 */
@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    
    /**
     * Find all workspaces by owner, ordered by creation date
     */
    List<Workspace> findByOwnerOrderByCreatedAtDesc(User owner);
    
    /**
     * Count workspaces by owner
     */
    long countByOwner(User owner);
    
    /**
     * Check if a workspace name exists for a user
     */
    boolean existsByOwnerAndName(User owner, String name);
}
