package com.example.taskmanagerapi.modules.workspaces.dto;

import jakarta.validation.constraints.NotBlank;

public record InviteMemberDTO(
    @NotBlank(message = "emailOrUsername is required")
    String emailOrUsername
) {}
