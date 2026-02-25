package com.example.taskmanagerapi.modules.auth.services;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.taskmanagerapi.modules.auth.domain.RefreshToken;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.repositories.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    /**
     * Create a new refresh token for a user
     * Refresh tokens expire in 7 days
     */
    public RefreshToken createRefreshToken(User user, String ipAddress, String userAgent) {
        // Delete any existing refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpirationDate(LocalDateTime.now().plusDays(7));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    /**
     * Validate and retrieve refresh token
     */
    public Optional<RefreshToken> validateRefreshToken(String token) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);
        
        if (refreshTokenOpt.isEmpty()) {
            return Optional.empty();
        }
        
        RefreshToken refreshToken = refreshTokenOpt.get();
        
        // Check if expired
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            return Optional.empty();
        }
        
        return Optional.of(refreshToken);
    }
    
    /**
     * Delete refresh token (logout)
     */
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
    
    /**
     * Delete all refresh tokens for a user (logout from all devices)
     */
    public void deleteAllUserTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
