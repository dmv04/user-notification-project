package dev.dmv04.userservice.service;

import dev.dmv04.userservice.dto.CreateUserRequest;
import dev.dmv04.userservice.dto.UpdateUserRequest;
import dev.dmv04.userservice.dto.UserDTO;
import dev.dmv04.userservice.entity.User;
import dev.dmv04.userservice.exception.EmailAlreadyExistsException;
import dev.dmv04.userservice.exception.UserNotFoundException;
import dev.dmv04.userservice.producer.UserEventProducer;
import dev.dmv04.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEventProducer userEventProducer;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_shouldThrowWhenEmailAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest("Alice", "existing@test.com", 30);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email 'existing@test.com' already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_shouldSaveAndReturnDto() {
        CreateUserRequest request = new CreateUserRequest("Bob", "bob@test.com", 25);
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("Bob");
        savedUser.setEmail("bob@test.com");
        savedUser.setAge(25);
        savedUser.setCreatedAt(LocalDateTime.now());

        when(userRepository.existsByEmail("bob@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDTO result = userService.createUser(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Bob");
        assertThat(result.email()).isEqualTo("bob@test.com");
        assertThat(result.age()).isEqualTo(25);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(999L, new UpdateUserRequest("New", "new@test.com", 30)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 999 not found");

    }

    @Test
    void updateUser_shouldThrowWhenNewEmailAlreadyExists() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("Old");
        existing.setEmail("old@test.com");
        existing.setAge(40);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@test.com")).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest("New", "new@test.com", 50);

        assertThatThrownBy(() -> userService.updateUser(1L, request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email 'new@test.com' already exists");

    }

    @Test
    void updateUser_shouldSkipNameIfNullOrBlank() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("Old Name");
        existing.setEmail("old@test.com");
        existing.setAge(30);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);

        UpdateUserRequest request = new UpdateUserRequest("", "new@test.com", 40);

        UserDTO result = userService.updateUser(1L, request);

        assertThat(result.name()).isEqualTo("Old Name");
        assertThat(result.email()).isEqualTo("new@test.com");
        assertThat(result.age()).isEqualTo(40);
    }

    @Test
    void updateUser_shouldSkipEmailIfNullOrBlank() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("Old");
        existing.setEmail("old@test.com");
        existing.setAge(40);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);

        UpdateUserRequest request = new UpdateUserRequest("New", "", 50);

        UserDTO result = userService.updateUser(1L, request);

        assertThat(result.name()).isEqualTo("New");
        assertThat(result.email()).isEqualTo("old@test.com");
        assertThat(result.age()).isEqualTo(50);
    }

    @Test
    void updateUser_shouldSkipAgeIfNull() {
        User existing = new User();
        existing.setId(1L);
        existing.setName("Old");
        existing.setEmail("old@test.com");
        existing.setAge(40);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);

        UpdateUserRequest request = new UpdateUserRequest("New", "new@test.com", null);

        UserDTO result = userService.updateUser(1L, request);

        assertThat(result.age()).isEqualTo(40);
    }

    @Test
    void deleteUser_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 999 not found");

        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteUser_shouldCallRepository() {
        User user = new User();
        user.setId(1L);
        user.setName("Test");
        user.setEmail("test@example.com");
        user.setAge(30);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void getUserById_shouldThrowWhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 999 not found");

    }
}
