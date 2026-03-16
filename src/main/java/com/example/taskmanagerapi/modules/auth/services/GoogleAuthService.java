package com.example.taskmanagerapi.modules.auth.services;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.repositories.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.RequiredArgsConstructor;

/**
 * Validates Google ID tokens issued by the frontend (e.g. Google Sign-In button)
 * and creates/retrieves the corresponding local user account.
 *
 * Flow:
 *   1. Frontend authenticates the user with Google and receives an id_token
 *   2. Frontend sends POST /auth/google { idToken }
 *   3. This service verifies the token against Google's public keys
 *   4. If the email already exists  → return that user (login)
 *   5. If the email doesn't exist   → create a new account with provider="google"
 */
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;

    @Value("${google.client-id}")
    private String googleClientId;

    /**
     * Verify the Google ID token and return the corresponding local user.
     * Creates the user automatically on first sign-in.
     *
     * @param idToken raw ID token string from the frontend
     * @return the local User entity
     * @throws IllegalArgumentException if the token is invalid or verification fails
     */
    @Transactional
    public User verifyAndGetUser(String idToken) {
        Payload payload = verifyToken(idToken);

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // Use email prefix as base username, ensuring uniqueness
        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");

        return userRepository.findByEmail(email)
                .map(existingUser -> handleExistingUser(existingUser, name))
                .orElseGet(() -> createGoogleUser(email, name, baseUsername));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Payload verifyToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new IllegalArgumentException("INVALID_GOOGLE_TOKEN");
            }
            return googleIdToken.getPayload();

        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException("INVALID_GOOGLE_TOKEN");
        }
    }

    /**
     * If the user already has a local account (password-based), we let them in
     * but do NOT change their provider — they can keep using both methods.
     * We do update their name if it changed in Google.
     */
    private User handleExistingUser(User user, String googleName) {
        if (googleName != null && !googleName.isBlank() && !googleName.equals(user.getName())) {
            user.setName(googleName);
            userRepository.save(user);
        }
        return user;
    }

    private User createGoogleUser(String email, String name, String baseUsername) {
        String username = resolveUniqueUsername(baseUsername);

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name != null ? name : username);
        newUser.setUsername(username);
        newUser.setPassword(null);           // no password for OAuth users
        newUser.setEmailVerified(true);      // Google already verified the email
        newUser.setProvider("google");

        return userRepository.save(newUser);
    }

    /**
     * Ensure the generated username doesn't collide with an existing one.
     * Appends a numeric suffix until unique: jonas, jonas_1, jonas_2, …
     */
    private String resolveUniqueUsername(String base) {
        if (userRepository.findByUsername(base).isEmpty()) {
            return base;
        }
        int suffix = 1;
        while (userRepository.findByUsername(base + "_" + suffix).isPresent()) {
            suffix++;
        }
        return base + "_" + suffix;
    }
}
