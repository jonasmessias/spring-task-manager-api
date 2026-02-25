package com.example.taskmanagerapi.modules.auth.dto;

public record ResetPasswordDTO(String token, String newPassword, String confirmNewPassword) {}
