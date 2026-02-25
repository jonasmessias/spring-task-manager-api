package com.example.taskmanagerapi.modules.tasks.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.tasks.domain.Task;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByOwner(User owner);
    List<Task> findByOwnerOrderByCreatedAtDesc(User owner);
}
