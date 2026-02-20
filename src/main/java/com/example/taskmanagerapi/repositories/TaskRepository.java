package com.example.taskmanagerapi.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.taskmanagerapi.domain.task.Task;
import com.example.taskmanagerapi.domain.user.User;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByUser(User user);
    List<Task> findByUserOrderByCreatedAtDesc(User user);
}
