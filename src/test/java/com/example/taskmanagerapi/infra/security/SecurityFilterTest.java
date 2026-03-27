package com.example.taskmanagerapi.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityFilter")
class SecurityFilterTest {

    @Mock private TokenService tokenService;

    @InjectMocks
    private SecurityFilter securityFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternal {

        @Test
        @DisplayName("should authenticate user with valid token")
        void shouldAuthenticateWithValidToken() throws Exception {
            Algorithm algorithm = Algorithm.HMAC256("test-secret");
            String token = JWT.create()
                    .withIssuer("login-auth-api")
                    .withSubject("john@test.com")
                    .withClaim("userId", "user-1")
                    .sign(algorithm);

            DecodedJWT decodedJWT = JWT.require(algorithm)
                    .withIssuer("login-auth-api")
                    .build()
                    .verify(token);

            request.addHeader("Authorization", "Bearer " + token);
            when(tokenService.validateToken(token)).thenReturn(decodedJWT);

            securityFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
            var principal = (com.example.taskmanagerapi.modules.auth.domain.User)
                    SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            assertThat(principal.getEmail()).isEqualTo("john@test.com");
            assertThat(principal.getId()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("should not authenticate when no token provided")
        void shouldNotAuthenticateWithoutToken() throws Exception {
            securityFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should not authenticate with invalid token")
        void shouldNotAuthenticateWithInvalidToken() throws Exception {
            request.addHeader("Authorization", "Bearer invalid-token");
            when(tokenService.validateToken("invalid-token")).thenReturn(null);

            securityFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}
