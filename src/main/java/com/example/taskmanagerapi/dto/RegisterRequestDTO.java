package com.example.taskmanagerapi.dto;

public record RegisterRequestDTO (String name, String email, String password, String confirmPassword) {}
