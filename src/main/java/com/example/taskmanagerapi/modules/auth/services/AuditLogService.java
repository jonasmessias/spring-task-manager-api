package com.example.taskmanagerapi.modules.auth.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.taskmanagerapi.modules.auth.domain.AuditLog;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.repositories.AuditLogRepository;

import lombok.RequiredArgsConstructor;

/**
 * AuditLogService — persists authentication events to the database
 * and writes structured log entries for monitoring tools.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository auditLogRepository;

    /**
     * Log user login event
     */
    public void logLogin(User user, String ipAddress, String userAgent) {
        persist("LOGIN", user.getEmail(), ipAddress, userAgent, null);
        logger.info("[LOGIN] User: {} | Email: {} | IP: {}", user.getName(), user.getEmail(), ipAddress);
    }
    
    /**
     * Log user logout event
     */
    public void logLogout(User user, String ipAddress) {
        persist("LOGOUT", user.getEmail(), ipAddress, null, null);
        logger.info("[LOGOUT] User: {} | Email: {} | IP: {}", user.getName(), user.getEmail(), ipAddress);
    }
    
    /**
     * Log user registration event
     */
    public void logRegistration(User user, String ipAddress) {
        persist("REGISTER", user.getEmail(), ipAddress, null, null);
        logger.info("[REGISTER] User: {} | Email: {} | IP: {}", user.getName(), user.getEmail(), ipAddress);
    }
    
    /**
     * Log token refresh event
     */
    public void logTokenRefresh(User user, String ipAddress) {
        persist("TOKEN_REFRESH", user.getEmail(), ipAddress, null, null);
        logger.info("[TOKEN_REFRESH] User: {} | Email: {} | IP: {}", user.getName(), user.getEmail(), ipAddress);
    }
    
    /**
     * Log logout from all devices event
     */
    public void logLogoutAll(User user, String ipAddress) {
        persist("LOGOUT_ALL", user.getEmail(), ipAddress, null, "User initiated global logout");
        logger.warn("[LOGOUT_ALL] User: {} | Email: {} | IP: {}", user.getName(), user.getEmail(), ipAddress);
    }
    
    /**
     * Log password reset event (with token revocation)
     */
    public void logPasswordReset(User user, String ipAddress) {
        persist("PASSWORD_RESET", user.getEmail(), ipAddress, null, "All refresh tokens invalidated");
        logger.warn("[PASSWORD_RESET] User: {} | Email: {} | IP: {}", user.getName(), user.getEmail(), ipAddress);
    }

    /**
     * Log email verification event
     */
    public void logEmailVerification(User user, String ipAddress) {
        persist("EMAIL_VERIFIED", user.getEmail(), ipAddress, null, null);
        logger.info("[EMAIL_VERIFIED] User: {} | Email: {} | IP: {}", user.getName(), user.getEmail(), ipAddress);
    }

    // -------------------------------------------------------------------------
    // Private helper
    // -------------------------------------------------------------------------
    
    private void persist(String action, String email, String ipAddress, String userAgent, String details) {
        try {
            AuditLog log = AuditLog.of(action, email, ipAddress, userAgent, details);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Never let audit persistence break the main flow
            logger.error("[AUDIT_ERROR] Failed to persist audit log: action={}, email={}, error={}", 
                action, email, e.getMessage());
        }
    }
}
