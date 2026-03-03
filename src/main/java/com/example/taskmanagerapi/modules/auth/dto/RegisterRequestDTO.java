package com.example.taskmanagerapi.modules.auth.dto;

public record RegisterRequestDTO(String name, String username, String email, String password, String confirmPassword) {}
