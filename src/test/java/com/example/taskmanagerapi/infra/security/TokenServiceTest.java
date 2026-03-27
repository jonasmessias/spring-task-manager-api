package com.example.taskmanagerapi.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.taskmanagerapi.modules.auth.domain.User;

@DisplayName("TokenService")
class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret-key-1234567890");
    }

    private User createUser(String id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("should generate a valid JWT token")
        void shouldGenerateValidToken() {
            User user = createUser("user-1", "john@test.com");

            String token = tokenService.generateToken(user);

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("should contain correct subject (email)")
        void shouldContainCorrectSubject() {
            User user = createUser("user-1", "john@test.com");

            String token = tokenService.generateToken(user);
            DecodedJWT decoded = tokenService.validateToken(token);

            assertThat(decoded).isNotNull();
            assertThat(decoded.getSubject()).isEqualTo("john@test.com");
        }

        @Test
        @DisplayName("should contain userId claim")
        void shouldContainUserIdClaim() {
            User user = createUser("user-123", "john@test.com");

            String token = tokenService.generateToken(user);
            DecodedJWT decoded = tokenService.validateToken(token);

            assertThat(decoded).isNotNull();
            assertThat(decoded.getClaim("userId").asString()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("should contain issuer")
        void shouldContainIssuer() {
            User user = createUser("user-1", "john@test.com");

            String token = tokenService.generateToken(user);
            DecodedJWT decoded = tokenService.validateToken(token);

            assertThat(decoded).isNotNull();
            assertThat(decoded.getIssuer()).isEqualTo("login-auth-api");
        }

        @Test
        @DisplayName("should have expiration date set")
        void shouldHaveExpirationDate() {
            User user = createUser("user-1", "john@test.com");

            String token = tokenService.generateToken(user);
            DecodedJWT decoded = tokenService.validateToken(token);

            assertThat(decoded).isNotNull();
            assertThat(decoded.getExpiresAt()).isNotNull();
            assertThat(decoded.getExpiresAt()).isAfter(java.util.Date.from(java.time.Instant.now()));
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("should return DecodedJWT for valid token")
        void shouldReturnDecodedJWTForValidToken() {
            User user = createUser("user-1", "john@test.com");
            String token = tokenService.generateToken(user);

            DecodedJWT result = tokenService.validateToken(token);

            assertThat(result).isNotNull();
            assertThat(result.getSubject()).isEqualTo("john@test.com");
        }

        @Test
        @DisplayName("should return null for invalid token")
        void shouldReturnNullForInvalidToken() {
            DecodedJWT result = tokenService.validateToken("invalid.token.here");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for empty token")
        void shouldReturnNullForEmptyToken() {
            DecodedJWT result = tokenService.validateToken("");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for token signed with different secret")
        void shouldReturnNullForTokenWithDifferentSecret() {
            User user = createUser("user-1", "john@test.com");
            String token = tokenService.generateToken(user);

            TokenService otherService = new TokenService();
            ReflectionTestUtils.setField(otherService, "secret", "different-secret-key");

            DecodedJWT result = otherService.validateToken(token);

            assertThat(result).isNull();
        }
    }
}
