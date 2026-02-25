package com.example.taskmanagerapi.domain.refreshtoken;

import java.time.LocalDateTime;

import com.example.taskmanagerapi.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    
    @Id
    private String token;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDateTime expirationDate;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private String ipAddress;
    
    private String userAgent;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expirationDate);
    }
}
