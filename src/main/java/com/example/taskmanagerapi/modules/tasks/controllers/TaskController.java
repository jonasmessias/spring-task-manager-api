package com.example.taskmanagerapi.modules.tasks.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.tasks.domain.Task;
import com.example.taskmanagerapi.modules.tasks.dto.CreateTaskDTO;
import com.example.taskmanagerapi.modules.tasks.dto.TaskResponseDTO;
import com.example.taskmanagerapi.modules.tasks.dto.UpdateTaskDTO;
import com.example.taskmanagerapi.modules.tasks.repositories.TaskRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Endpoints for managing user tasks")
@SecurityRequirement(name = "Bearer Authentication")
public class TaskController {
    private final TaskRepository taskRepository;

    @Operation(summary = "Create Task", description = "Create a new task for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Task created successfully",
                content = @Content(schema = @Schema(implementation = TaskResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PostMapping
    public ResponseEntity<Object> createTask(
            @RequestBody CreateTaskDTO body,
            @AuthenticationPrincipal User user) {
        
        Task task = new Task();
        task.setTitle(body.title());
        task.setDescription(body.description());
        task.setOwner(user);
        task.setCreatedAt(LocalDateTime.now());
        
        Task savedTask = this.taskRepository.save(task);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TaskResponseDTO(savedTask));
    }

    @Operation(summary = "Get All Tasks", description = "Retrieve all tasks for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    public ResponseEntity<Object> getAllTasks(@AuthenticationPrincipal User user) {
        
        List<Task> tasks = this.taskRepository.findByOwnerOrderByCreatedAtDesc(user);
        List<TaskResponseDTO> response = tasks.stream()
                .map(TaskResponseDTO::new)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Task by ID", description = "Retrieve a specific task by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task found",
                content = @Content(schema = @Schema(implementation = TaskResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Task not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Task belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Object> getTaskById(
            @Parameter(description = "Task ID", required = true) @PathVariable @NonNull String id,
            @AuthenticationPrincipal User user) {
        
        Optional<Task> taskOpt = this.taskRepository.findById(id);
        
        if (taskOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Task not found");
        }
        
        Task task = taskOpt.get();
        
        // Check if task belongs to the authenticated user
        if (!task.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to access this task");
        }
        
        return ResponseEntity.ok(new TaskResponseDTO(task));
    }

    @Operation(summary = "Update Task", description = "Update an existing task (title, description, or status)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task updated successfully",
                content = @Content(schema = @Schema(implementation = TaskResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Task not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Task belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateTask(
            @Parameter(description = "Task ID", required = true) @PathVariable @NonNull String id,
            @RequestBody UpdateTaskDTO body,
            @AuthenticationPrincipal User user) {
        
        Optional<Task> taskOpt = this.taskRepository.findById(id);
        
        if (taskOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Task not found");
        }
        
        Task task = taskOpt.get();
        
        // Check if task belongs to the authenticated user
        if (!task.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to update this task");
        }
        
        // Update fields
        if (body.title() != null) {
            task.setTitle(body.title());
        }
        if (body.description() != null) {
            task.setDescription(body.description());
        }
        if (body.status() != null) {
            task.setStatus(body.status());
        }
        task.setUpdatedAt(LocalDateTime.now());
        
        Task updatedTask = this.taskRepository.save(task);
        
        return ResponseEntity.ok(new TaskResponseDTO(updatedTask));
    }

    @Operation(summary = "Delete Task", description = "Delete a task by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Task not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Task belongs to another user"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteTask(
            @Parameter(description = "Task ID", required = true) @PathVariable @NonNull String id,
            @AuthenticationPrincipal User user) {
        
        Optional<Task> taskOpt = this.taskRepository.findById(id);
        
        if (taskOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Task not found");
        }
        
        Task task = taskOpt.get();
        
        // Check if task belongs to the authenticated user
        if (!task.getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to delete this task");
        }
        
        this.taskRepository.delete(task);
        
        return ResponseEntity.ok("Task deleted successfully");
    }
}
