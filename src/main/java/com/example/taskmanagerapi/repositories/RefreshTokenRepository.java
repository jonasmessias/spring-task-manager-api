package com.example.taskmanagerapi.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.domain.refreshtoken.RefreshToken;
import com.example.taskmanagerapi.domain.user.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    
    Optional<RefreshToken> findByToken(String token);
    
    @Modifying
    @Transactional
    void deleteByUser(User user);
    
    @Modifying
    @Transactional
    void deleteByToken(String token);
}
