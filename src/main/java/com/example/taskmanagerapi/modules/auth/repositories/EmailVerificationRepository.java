package com.example.taskmanagerapi.modules.auth.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.taskmanagerapi.modules.auth.domain.EmailVerificationToken;

public interface EmailVerificationRepository extends JpaRepository<EmailVerificationToken, String> {
    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByEmail(String email);
}
