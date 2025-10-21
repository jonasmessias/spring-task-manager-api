package com.example.taskmanagerapi.repositories;

import com.example.taskmanagerapi.domain.passwordreset.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,String> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByToken(String token);
}
