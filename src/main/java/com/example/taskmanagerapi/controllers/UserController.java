package com.example.taskmanagerapi.controllers;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskmanagerapi.domain.user.User;
import com.example.taskmanagerapi.dto.UserProfileDTO;

@RestController
@RequestMapping("/me")
public class UserController {
    
    @GetMapping
    public ResponseEntity<Object> getUser(@AuthenticationPrincipal User user){
        UserProfileDTO profile = new UserProfileDTO(
            user.getId(),
            user.getName(),
            user.getEmail() 
        );
        return ResponseEntity.ok(profile);
    }
}
