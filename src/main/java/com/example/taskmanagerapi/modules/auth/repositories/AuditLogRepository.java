package com.example.taskmanagerapi.modules.auth.repositories;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.taskmanagerapi.modules.auth.domain.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    
    List<AuditLog> findByEmailOrderByTimestampDesc(String email);
    
    Page<AuditLog> findByEmailOrderByTimestampDesc(String email, Pageable pageable);
    
    List<AuditLog> findByActionOrderByTimestampDesc(String action);
    
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(Instant start, Instant end);
}
