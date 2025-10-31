package dev.dmv04.userservice.repository;

import dev.dmv04.userservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.yml")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void save_shouldPersistUser() {
        User user = new User();
        user.setName("Alice");
        user.setEmail("alice@test.com");
        user.setAge(30);
        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Alice");
    }

    @Test
    void findById_shouldReturnUserWhenExists() {
        User user = new User();
        user.setName("Bob");
        user.setEmail("bob@test.com");
        user.setAge(25);
        user.setCreatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("bob@test.com");
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        Optional<User> found = userRepository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        User user1 = new User();
        user1.setName("User1");
        user1.setEmail("u1@test.com");
        user1.setAge(20);
        user1.setCreatedAt(LocalDateTime.now());

        User user2 = new User();
        user2.setName("User2");
        user2.setEmail("u2@test.com");
        user2.setAge(22);
        user2.setCreatedAt(LocalDateTime.now());

        userRepository.save(user1);
        userRepository.save(user2);

        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(2);
    }

    @Test
    void existsByEmail_shouldReturnTrueWhenEmailExists() {
        User user = new User();
        user.setName("Charlie");
        user.setEmail("charlie@test.com");
        user.setAge(40);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        boolean exists = userRepository.existsByEmail("charlie@test.com");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalseWhenEmailNotExists() {
        boolean exists = userRepository.existsByEmail("nonexistent@test.com");

        assertThat(exists).isFalse();
    }

    @Test
    void deleteById_shouldRemoveUser() {
        User user = new User();
        user.setName("ToDelete");
        user.setEmail("del@test.com");
        user.setAge(33);
        user.setCreatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        userRepository.deleteById(saved.getId());

        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }
}
