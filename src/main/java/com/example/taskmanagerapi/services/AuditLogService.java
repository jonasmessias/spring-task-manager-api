package com.example.taskmanagerapi.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.taskmanagerapi.domain.user.User;

@Service
public class AuditLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Log user login event
     */
    public void logLogin(User user, String ipAddress, String userAgent) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.info("[LOGIN] User: {} | Email: {} | IP: {} | Device: {} | Time: {}", 
            user.getName(), 
            user.getEmail(), 
            ipAddress != null ? ipAddress : "unknown",
            userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) : "unknown",
            timestamp
        );
    }
    
    /**
     * Log user logout event
     */
    public void logLogout(User user, String ipAddress) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.info("[LOGOUT] User: {} | Email: {} | IP: {} | Time: {}", 
            user.getName(), 
            user.getEmail(), 
            ipAddress != null ? ipAddress : "unknown",
            timestamp
        );
    }
    
    /**
     * Log user registration event
     */
    public void logRegistration(User user, String ipAddress) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.info("[REGISTER] User: {} | Email: {} | IP: {} | Time: {}", 
            user.getName(), 
            user.getEmail(), 
            ipAddress != null ? ipAddress : "unknown",
            timestamp
        );
    }
    
    /**
     * Log token refresh event
     */
    public void logTokenRefresh(User user, String ipAddress) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.info("[TOKEN_REFRESH] User: {} | Email: {} | IP: {} | Time: {}", 
            user.getName(), 
            user.getEmail(), 
            ipAddress != null ? ipAddress : "unknown",
            timestamp
        );
    }
    
    /**
     * Log logout from all devices event
     */
    public void logLogoutAll(User user, String ipAddress) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.warn("[LOGOUT_ALL] User: {} | Email: {} | IP: {} | Time: {} | Reason: User initiated global logout", 
            user.getName(), 
            user.getEmail(), 
            ipAddress != null ? ipAddress : "unknown",
            timestamp
        );
    }
    
    /**
     * Log password reset event (with token revocation)
     */
    public void logPasswordReset(User user, String ipAddress) {
        String timestamp = LocalDateTime.now().format(formatter);
        logger.warn("[PASSWORD_RESET] User: {} | Email: {} | IP: {} | Time: {} | Action: All refresh tokens invalidated", 
            user.getName(), 
            user.getEmail(), 
            ipAddress != null ? ipAddress : "unknown",
            timestamp
        );
    }
}
