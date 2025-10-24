package com.example.taskmanagerapi.dto;

public record ResetPasswordDTO(String token, String newPassword, String confirmNewPassword) {}
