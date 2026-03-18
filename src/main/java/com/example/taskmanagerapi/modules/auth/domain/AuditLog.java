package com.example.taskmanagerapi.modules.auth.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AuditLog — persists every authentication event (login, logout, register, etc.)
 * for security auditing and analytics.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private String id;

    /** Action type: LOGIN, LOGOUT, REGISTER, TOKEN_REFRESH, LOGOUT_ALL, PASSWORD_RESET, EMAIL_VERIFIED */
    @Column(nullable = false, length = 30)
    private String action;

    /** User email at the time of the event */
    @Column(nullable = false)
    private String email;

    /** Client IP address */
    @Column(length = 45)
    private String ipAddress;

    /** User-Agent header (truncated) */
    @Column(length = 255)
    private String userAgent;

    /** Extra details (e.g. "All refresh tokens invalidated") */
    @Column(length = 500)
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    /**
     * Factory method for convenience
     */
    public static AuditLog of(String action, String email, String ipAddress, String userAgent, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEmail(email);
        log.setIpAddress(ipAddress != null ? ipAddress : "unknown");
        log.setUserAgent(userAgent != null ? userAgent.substring(0, Math.min(255, userAgent.length())) : null);
        log.setDetails(details);
        log.setTimestamp(Instant.now());
        return log;
    }
}
