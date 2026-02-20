package com.example.taskmanagerapi.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.domain.passwordreset.PasswordResetToken;

public interface PasswordResetRepository extends JpaRepository<PasswordResetToken,String> {
    Optional<PasswordResetToken> findByToken(String token);
    
    @Modifying
    @Transactional
    void deleteByEmail(String email);
}
