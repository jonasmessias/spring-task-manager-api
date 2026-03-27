package com.example.taskmanagerapi.modules.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.example.taskmanagerapi.infra.exception.ConflictException;
import com.example.taskmanagerapi.infra.exception.ResourceNotFoundException;
import com.example.taskmanagerapi.modules.auth.domain.User;
import com.example.taskmanagerapi.modules.auth.dto.UpdateProfileDTO;
import com.example.taskmanagerapi.modules.auth.dto.UserProfileDTO;
import com.example.taskmanagerapi.modules.auth.repositories.UserRepository;
import com.example.taskmanagerapi.modules.storage.services.StorageService;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StorageService storageService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User jwtUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setName("John Doe");
        testUser.setUsername("johndoe");
        testUser.setEmail("john@test.com");
        testUser.setAvatarUrl(null);

        jwtUser = new User();
        jwtUser.setId("user-1");
        jwtUser.setEmail("john@test.com");
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("should return user profile")
        void shouldReturnProfile() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));

            UserProfileDTO result = userService.getProfile(jwtUser);

            assertThat(result.id()).isEqualTo("user-1");
            assertThat(result.name()).isEqualTo("John Doe");
            assertThat(result.username()).isEqualTo("johndoe");
            assertThat(result.email()).isEqualTo("john@test.com");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById("user-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile(jwtUser))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("should update name and username")
        void shouldUpdateProfile() {
            UpdateProfileDTO dto = new UpdateProfileDTO("Jane Doe", "janedoe");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(userRepository.findByUsername("janedoe")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserProfileDTO result = userService.updateProfile(jwtUser, dto);

            assertThat(result).isNotNull();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw ConflictException when username already taken")
        void shouldThrowWhenUsernameTaken() {
            User otherUser = new User();
            otherUser.setId("user-2");
            otherUser.setUsername("janedoe");

            UpdateProfileDTO dto = new UpdateProfileDTO("Jane Doe", "janedoe");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(userRepository.findByUsername("janedoe")).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> userService.updateProfile(jwtUser, dto))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("should skip username update if same as current")
        void shouldSkipSameUsername() {
            UpdateProfileDTO dto = new UpdateProfileDTO("New Name", "johndoe");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserProfileDTO result = userService.updateProfile(jwtUser, dto);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @Test
        @DisplayName("should delete account without avatar")
        void shouldDeleteWithoutAvatar() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));

            userService.deleteAccount(jwtUser);

            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("should delete account and avatar from storage")
        void shouldDeleteWithAvatar() {
            testUser.setAvatarUrl("https://s3.amazonaws.com/avatars/user-1.jpg");
            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));

            userService.deleteAccount(jwtUser);

            verify(storageService).deleteFile("https://s3.amazonaws.com/avatars/user-1.jpg");
            verify(userRepository).delete(testUser);
        }
    }

    @Nested
    @DisplayName("uploadAvatar")
    class UploadAvatar {

        @Test
        @DisplayName("should upload new avatar")
        void shouldUploadAvatar() {
            MockMultipartFile file = new MockMultipartFile(
                    "avatar", "photo.jpg", "image/jpeg", "fake-image".getBytes());

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(storageService.uploadFile(file, "avatars")).thenReturn("https://s3.amazonaws.com/avatars/new.jpg");
            testUser.setAvatarUrl("https://s3.amazonaws.com/avatars/new.jpg");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserProfileDTO result = userService.uploadAvatar(jwtUser, file);

            assertThat(result.avatarUrl()).isEqualTo("https://s3.amazonaws.com/avatars/new.jpg");
        }

        @Test
        @DisplayName("should delete old avatar before uploading new one")
        void shouldDeleteOldAvatar() {
            testUser.setAvatarUrl("https://s3.amazonaws.com/avatars/old.jpg");
            MockMultipartFile file = new MockMultipartFile(
                    "avatar", "photo.jpg", "image/jpeg", "fake-image".getBytes());

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(storageService.uploadFile(file, "avatars")).thenReturn("https://s3.amazonaws.com/avatars/new.jpg");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.uploadAvatar(jwtUser, file);

            verify(storageService).deleteFile("https://s3.amazonaws.com/avatars/old.jpg");
        }
    }

    @Nested
    @DisplayName("deleteAvatar")
    class DeleteAvatar {

        @Test
        @DisplayName("should delete avatar and set null")
        void shouldDeleteAvatar() {
            testUser.setAvatarUrl("https://s3.amazonaws.com/avatars/user-1.jpg");
            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.deleteAvatar(jwtUser);

            verify(storageService).deleteFile("https://s3.amazonaws.com/avatars/user-1.jpg");
            assertThat(testUser.getAvatarUrl()).isNull();
        }

        @Test
        @DisplayName("should do nothing if no avatar exists")
        void shouldDoNothingIfNoAvatar() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));

            userService.deleteAvatar(jwtUser);

            assertThat(testUser.getAvatarUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return user profile by ID")
        void shouldReturnUserById() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));

            UserProfileDTO result = userService.getUserById("user-1");

            assertThat(result.id()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById("unknown"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
