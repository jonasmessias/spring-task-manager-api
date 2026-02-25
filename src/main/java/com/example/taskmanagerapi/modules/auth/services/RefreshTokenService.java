package com.example.taskmanagerapi.modules.auth.services;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.modules.auth.domain.RefreshToken;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.repositories.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String REFRESH_TOKEN_CACHE_PREFIX = "refresh_token:";
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;
    
    /**
     * Create a new refresh token for a user
     * Refresh tokens expire in 7 days and are cached in Redis
     */
    @Transactional
    public RefreshToken createRefreshToken(User user, String ipAddress, String userAgent) {
        // Delete any existing refresh tokens for this user (both DB and cache)
        deleteAllUserTokens(user);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpirationDate(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        
        // Save to database
        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        
        // Cache in Redis with 7-day expiration
        cacheRefreshToken(savedToken);
        
        log.debug("Created and cached refresh token for user: {}", user.getEmail());
        
        return savedToken;
    }
    
    /**
     * Validate and retrieve refresh token
     * Checks Redis cache first, falls back to database
     */
    public Optional<RefreshToken> validateRefreshToken(String token) {
        String cacheKey = REFRESH_TOKEN_CACHE_PREFIX + token;
        
        // Try cache first
        try {
            RefreshToken cachedToken = (RefreshToken) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedToken != null) {
                // Verify token is not expired
                if (!cachedToken.isExpired()) {
                    log.debug("Refresh token found in cache: {}", token.substring(0, 8) + "...");
                    
                    // Update cache expiration on successful validation
                    updateCacheExpiration(cachedToken);
                    
                    return Optional.of(cachedToken);
                } else {
                    // Token expired, remove from cache and DB
                    log.debug("Cached refresh token expired: {}", token.substring(0, 8) + "...");
                    deleteRefreshToken(token);
                    return Optional.empty();
                }
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed, falling back to database: {}", e.getMessage());
        }
        
        // Cache miss - check database
        log.debug("Cache miss, checking database for token: {}", token.substring(0, 8) + "...");
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByToken(token);
        
        if (refreshTokenOpt.isEmpty()) {
            return Optional.empty();
        }
        
        RefreshToken refreshToken = refreshTokenOpt.get();
        
        // Check if expired
        if (refreshToken.isExpired()) {
            log.debug("Database refresh token expired: {}", token.substring(0, 8) + "...");
            refreshTokenRepository.delete(refreshToken);
            return Optional.empty();
        }
        
        // Cache the token for future requests
        cacheRefreshToken(refreshToken);
        log.debug("Token retrieved from DB and cached: {}", token.substring(0, 8) + "...");
        
        return Optional.of(refreshToken);
    }
    
    /**
     * Delete refresh token (logout from specific device)
     * Removes from both cache and database
     */
    @Transactional
    public void deleteRefreshToken(String token) {
        String cacheKey = REFRESH_TOKEN_CACHE_PREFIX + token;
        
        // Remove from cache
        try {
            redisTemplate.delete(cacheKey);
            log.debug("Deleted refresh token from cache: {}", token.substring(0, 8) + "...");
        } catch (Exception e) {
            log.warn("Failed to delete token from cache: {}", e.getMessage());
        }
        
        // Remove from database
        refreshTokenRepository.deleteByToken(token);
        log.debug("Deleted refresh token from database: {}", token.substring(0, 8) + "...");
    }
    
    /**
     * Delete all refresh tokens for a user (logout from all devices)
     * Clears both cache and database entries
     */
    @Transactional
    public void deleteAllUserTokens(User user) {
        // Get all tokens from database to remove from cache
        var tokens = refreshTokenRepository.findByUser(user);
        
        // Remove each token from cache
        tokens.forEach(token -> {
            try {
                String cacheKey = REFRESH_TOKEN_CACHE_PREFIX + token.getToken();
                redisTemplate.delete(cacheKey);
            } catch (Exception e) {
                log.warn("Failed to delete token from cache: {}", e.getMessage());
            }
        });
        
        // Remove all from database
        refreshTokenRepository.deleteByUser(user);
        
        log.info("Deleted all refresh tokens for user: {}", user.getEmail());
    }
    
    /**
     * Cache refresh token in Redis with expiration
     */
    private void cacheRefreshToken(RefreshToken token) {
        try {
            String cacheKey = REFRESH_TOKEN_CACHE_PREFIX + token.getToken();
            
            // Calculate remaining time until expiration
            long secondsUntilExpiry = java.time.Duration.between(
                LocalDateTime.now(), 
                token.getExpirationDate()
            ).getSeconds();
            
            // Only cache if token has more than 1 minute remaining
            if (secondsUntilExpiry > 60) {
                redisTemplate.opsForValue().set(
                    cacheKey, 
                    token, 
                    secondsUntilExpiry, 
                    TimeUnit.SECONDS
                );
                log.debug("Cached refresh token with {} seconds expiry", secondsUntilExpiry);
            }
        } catch (Exception e) {
            log.error("Failed to cache refresh token: {}", e.getMessage());
            // Don't fail the operation if caching fails
        }
    }
    
    /**
     * Update cache expiration time when token is validated/rotated
     * This implements a sliding window for frequently used tokens
     */
    private void updateCacheExpiration(RefreshToken token) {
        try {
            String cacheKey = REFRESH_TOKEN_CACHE_PREFIX + token.getToken();
            
            // Calculate remaining time
            long secondsUntilExpiry = java.time.Duration.between(
                LocalDateTime.now(), 
                token.getExpirationDate()
            ).getSeconds();
            
            // Update expiration
            if (secondsUntilExpiry > 60) {
                redisTemplate.expire(cacheKey, secondsUntilExpiry, TimeUnit.SECONDS);
                log.debug("Updated cache expiration for token");
            }
        } catch (Exception e) {
            log.warn("Failed to update cache expiration: {}", e.getMessage());
        }
    }
}
