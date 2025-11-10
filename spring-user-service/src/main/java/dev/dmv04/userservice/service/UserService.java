package dev.dmv04.userservice.service;

import dev.dmv04.userservice.dto.CreateUserRequest;
import dev.dmv04.userservice.dto.UpdateUserRequest;
import dev.dmv04.userservice.dto.UserDTO;
import dev.dmv04.userservice.dto.UserEvent;
import dev.dmv04.userservice.entity.User;
import dev.dmv04.userservice.exception.EmailAlreadyExistsException;
import dev.dmv04.userservice.exception.UserNotFoundException;
import dev.dmv04.userservice.producer.UserEventProducer;
import dev.dmv04.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserEventProducer userEventProducer;

    public UserService(UserRepository userRepository, UserEventProducer userEventProducer) {
        this.userRepository = userRepository;
        this.userEventProducer = userEventProducer;
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAllByOrderByIdAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setAge(request.age());
        User saved = userRepository.save(user);

        userEventProducer.sendUserEvent(user.getEmail(), UserEvent.CREATE);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toDto(user);
    }

    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String newEmail = request.email().trim();
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new EmailAlreadyExistsException(newEmail);
            }
            user.setEmail(newEmail);
        }

        if (request.age() != null) {
            user.setAge(request.age());
        }

        User updated = userRepository.save(user);
        return toDto(updated);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userRepository.deleteById(id);

        userEventProducer.sendUserEvent(user.getEmail(), UserEvent.DELETE);
    }

    private UserDTO toDto(User user) {
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getCreatedAt()
        );
    }
}
