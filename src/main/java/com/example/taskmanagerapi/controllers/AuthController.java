package com.example.taskmanagerapi.controllers;

import com.example.taskmanagerapi.domain.passwordreset.PasswordResetToken;
import com.example.taskmanagerapi.domain.user.User;
import com.example.taskmanagerapi.dto.*;
import com.example.taskmanagerapi.infra.security.TokenService;
import com.example.taskmanagerapi.repositories.PasswordResetRepository;
import com.example.taskmanagerapi.repositories.UserRepository;
import com.example.taskmanagerapi.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PasswordResetRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody LoginRequestDTO body){
        User user = this.repository.findByEmail(body.email()).orElseThrow(() -> new RuntimeException("User not found"));
        if(passwordEncoder.matches(body.password(), user.getPassword())){
            String token = tokenService.generateToken(user);
            return ResponseEntity.ok(new ResponseDTO(user.getName(), token));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody RegisterRequestDTO body){

        if(!body.password().equals(body.confirmPassword())){
            return ResponseEntity.badRequest().body("Passwords do not match");
        }

        Optional<User> user = this.repository.findByEmail(body.email());

        if(user.isEmpty()) {
            User newUser = new User();
            newUser.setPassword(passwordEncoder.encode(body.password()));
            newUser.setEmail(body.email());
            newUser.setName(body.name());
            this.repository.save(newUser);

            String token = tokenService.generateToken(newUser);
            return ResponseEntity.ok(new ResponseDTO(newUser.getName(), token));
        }
        return ResponseEntity.badRequest().body("Email already registered");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity forgotPassword(@RequestBody ForgotPasswordRequestDTO body) {

        Optional<User> userOpt = this.repository.findByEmail(body.email());

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("E-mail not found.");
        }

        String token = UUID.randomUUID().toString();
        passwordResetTokenRepository.deleteByEmail(body.email());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(body.email());
        resetToken.setToken(token);
        resetToken.setExpirationDate(LocalDateTime.now().plusMinutes(30));
        passwordResetTokenRepository.save(resetToken);

        String resetLink = "http://localhost:4200/reset-password?token=" + token;
        String message = "Olá, " + userOpt.get().getName() + "!\n\n" +
                "Clique no link abaixo para redefinir sua senha:\n" +
                resetLink + "\n\n" +
                "O link expira em 30 minutos.\n\nTask Manager";

        emailService.sendEmail(body.email(), "Redefinição de senha - Task Manager", message);

        return ResponseEntity.ok("E-mail de redefinição enviado para: " + body.email());
    }

    @PostMapping("/reset-password")
    public ResponseEntity resetPassword(@RequestBody ResetPasswordDTO body) {

        if(!body.newPassword().equals(body.confirmNewPassword())) {
            return ResponseEntity.badRequest().body("Passwords do not match");
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(body.token());

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid token.");
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Expired token.");
        }

        Optional<User> userOpt = repository.findByEmail(resetToken.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found.");
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(body.newPassword()));
        repository.save(user);
        passwordResetTokenRepository.delete(resetToken);

        return ResponseEntity.ok("Password reset complete!");
    }
}
