package com.example.taskmanagerapi.modules.workspaces.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.workspaces.domain.Workspace;
import com.example.taskmanagerapi.modules.workspaces.domain.WorkspaceMember;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, String> {

    List<WorkspaceMember> findByWorkspace(Workspace workspace);

    Optional<WorkspaceMember> findByWorkspaceAndUser(Workspace workspace, User user);

    boolean existsByWorkspaceAndUser(Workspace workspace, User user);

    void deleteByWorkspaceAndUser(Workspace workspace, User user);
}
