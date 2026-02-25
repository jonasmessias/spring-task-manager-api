package com.example.taskmanagerapi.infra.security;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.taskmanagerapi.modules.auth.domain.User;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;
    
     public String generateToken(User user){
         try {
             Algorithm algorithm = Algorithm.HMAC256(secret);

             String token = JWT.create()
                     .withIssuer("login-auth-api")
                     .withSubject(user.getEmail())
                     .withExpiresAt(this.generateExpirationDate())
                     .sign(algorithm);
             return token;
         } catch (JWTCreationException exception) {
             throw new RuntimeException("Error while authenticating token");
         }
     }

     public String validadeToken(String token){
         try {
             Algorithm algorithm = Algorithm.HMAC256(secret);
             return JWT.require(algorithm)
                     .withIssuer("login-auth-api")
                     .build()
                     .verify(token)
                     .getSubject();
         } catch (JWTVerificationException exception) {
             return null;
         }
     }

     private Instant generateExpirationDate() {
         // Access token expires in 4 hours (optimized for long-lived task management sessions)
         // Reduces token refresh requests by ~16x compared to 15 minutes
         return LocalDateTime.now().plusHours(4).toInstant(ZoneOffset.of("-03:00"));
     }
}

