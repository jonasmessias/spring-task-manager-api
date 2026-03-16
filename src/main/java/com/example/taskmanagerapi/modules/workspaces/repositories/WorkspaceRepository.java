package com.example.taskmanagerapi.modules.workspaces.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Find all workspaces where the user is a member (owner or invited),
     * ordered by creation date descending
     */
    @Query("SELECT wm.workspace FROM WorkspaceMember wm WHERE wm.user = :user ORDER BY wm.workspace.createdAt DESC")
    List<Workspace> findAllByMemberUser(@Param("user") User user);

    /**
     * Count workspaces by owner
     */
    long countByOwner(User owner);
    
    /**
     * Check if a workspace name exists for a user
     */
    boolean existsByOwnerAndName(User owner, String name);
}
