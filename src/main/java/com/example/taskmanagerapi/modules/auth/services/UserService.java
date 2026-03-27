package com.example.taskmanagerapi.modules.auth.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanagerapi.infra.exception.ConflictException;
import com.example.taskmanagerapi.infra.exception.ResourceNotFoundException;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.dto.UpdateProfileDTO;
import com.example.taskmanagerapi.modules.auth.dto.UserProfileDTO;
import com.example.taskmanagerapi.modules.auth.repositories.UserRepository;
import com.example.taskmanagerapi.modules.storage.services.StorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StorageService storageService;

    public User getFullUser(User jwtUser) {
        return userRepository.findById(jwtUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found."));
    }

    public UserProfileDTO getProfile(User user) {
        User fullUser = getFullUser(user);
        return toDTO(fullUser);
    }

    @Transactional
    public UserProfileDTO updateProfile(User jwtUser, UpdateProfileDTO dto) {
        User user = getFullUser(jwtUser);

        if (dto.username() != null && !dto.username().isBlank() && !user.getUsername().equals(dto.username())) {
            if (userRepository.findByUsername(dto.username()).isPresent()) {
                throw new ConflictException("USERNAME_TAKEN", "Username '" + dto.username() + "' is already taken.");
            }
            user.setUsername(dto.username());
        }

        if (dto.name() != null && !dto.name().isBlank()) {
            user.setName(dto.name());
        }

        User saved = userRepository.save(user);
        return toDTO(saved);
    }

    @Transactional
    public void deleteAccount(User jwtUser) {
        User user = getFullUser(jwtUser);
        if (user.getAvatarUrl() != null) {
            storageService.deleteFile(user.getAvatarUrl());
        }
        userRepository.delete(user);
    }

    @Transactional
    public UserProfileDTO uploadAvatar(User jwtUser, org.springframework.web.multipart.MultipartFile file) {
        User user = getFullUser(jwtUser);
        if (user.getAvatarUrl() != null) {
            storageService.deleteFile(user.getAvatarUrl());
        }

        String fileUrl = storageService.uploadFile(file, "avatars");
        user.setAvatarUrl(fileUrl);
        User saved = userRepository.save(user);
        return toDTO(saved);
    }

    @Transactional
    public void deleteAvatar(User jwtUser) {
        User user = getFullUser(jwtUser);
        if (user.getAvatarUrl() != null) {
            storageService.deleteFile(user.getAvatarUrl());
            user.setAvatarUrl(null);
            userRepository.save(user);
        }
    }

    public UserProfileDTO getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found."));
        return toDTO(user);
    }

    private UserProfileDTO toDTO(User user) {
        return new UserProfileDTO(
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.getEmail(),
            user.getAvatarUrl()
        );
    }
}
