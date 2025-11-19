package dev.dmv04.userservice.service;

import dev.dmv04.userservice.dto.CreateUserRequest;
import dev.dmv04.userservice.dto.UpdateUserRequest;
import dev.dmv04.userservice.dto.UserDTO;
import dev.dmv04.userservice.dto.UserEvent;
import dev.dmv04.userservice.exception.EmailAlreadyExistsException;
import dev.dmv04.userservice.exception.UserNotFoundException;
import dev.dmv04.userservice.repository.UserRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;

import static dev.dmv04.userservice.util.KafkaTestConsumerUtil.createConsumerForUserEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.3.0")
    );

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url",
                () -> "jdbc:tc:postgresql:15:///testdb");
        registry.add("spring.datasource.driver-class-name",
                () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldSendKafkaEventWhenUserCreated() {
        String topic = "user-events";
        CreateUserRequest request = new CreateUserRequest("Test User", "test@example.com", 25);

        userService.createUser(request);

        try (Consumer<String, UserEvent> consumer = createConsumerForUserEvent(kafka, topic)) {
            ConsumerRecords<String, UserEvent> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

            assertThat(records).isNotEmpty();

            var record = records.iterator().next();
            assertThat(record.value().email()).isEqualTo("test@example.com");
            assertThat(record.value().action()).isEqualTo("CREATE");
        }
    }

    @Test
    void shouldSendKafkaEventWhenUserDeleted() {
        String topic = "user-events";

        try (Consumer<String, UserEvent> consumer = createConsumerForUserEvent(kafka, topic)) {
            CreateUserRequest createRequest = new CreateUserRequest("Test User", "test@example.com", 25);
            var user = userService.createUser(createRequest);

            ConsumerRecords<String, UserEvent> createRecords =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5));
            assertThat(createRecords).isNotEmpty();

            userService.deleteUser(user.id());

            ConsumerRecords<String, UserEvent> deleteRecords =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

            assertThat(deleteRecords).isNotEmpty();

            var record = deleteRecords.iterator().next();
            assertThat(record.value().email()).isEqualTo("test@example.com");
            assertThat(record.value().action()).isEqualTo("DELETE");
        }
    }

    @Test
    void shouldCreateUserSuccessfullyWithCircuitBreaker() {
        CreateUserRequest request = new CreateUserRequest("Test User", "test@example.com", 25);

        UserDTO user = userService.createUser(request);

        assertThat(user).isNotNull();
        assertThat(user.name()).isEqualTo("Test User");
        assertThat(user.email()).isEqualTo("test@example.com");
        assertThat(user.age()).isEqualTo(25);
        assertThat(user.createdAt()).isNotNull();
    }

    @Test
    void shouldDeleteUserSuccessfullyWithCircuitBreaker() {
        String topic = "user-events";
        String uniqueEmail = "delete-test-" + System.currentTimeMillis() + "@example.com";

        CreateUserRequest createRequest = new CreateUserRequest("Test User", uniqueEmail, 25);
        UserDTO createdUser = userService.createUser(createRequest);

        try (Consumer<String, UserEvent> consumer = createConsumerForUserEvent(kafka, topic)) {
            ConsumerRecords<String, UserEvent> createRecords =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

            boolean createEventFound = false;
            for (ConsumerRecord<String, UserEvent> record : createRecords) {
                if (record.value().email().equals(uniqueEmail) &&
                        record.value().action().equals("CREATE")) {
                    createEventFound = true;
                    break;
                }
            }
            assertThat(createEventFound).isTrue();
        }

        userService.deleteUser(createdUser.id());

        assertThat(userRepository.findById(createdUser.id())).isEmpty();

        try (Consumer<String, UserEvent> deleteConsumer = createConsumerForUserEvent(kafka, topic)) {
            ConsumerRecords<String, UserEvent> allRecords =
                    KafkaTestUtils.getRecords(deleteConsumer, Duration.ofSeconds(15));

            assertThat(allRecords).isNotEmpty();

            boolean deleteEventFound = false;
            for (ConsumerRecord<String, UserEvent> record : allRecords) {
                if (record.value().email().equals(uniqueEmail) &&
                        record.value().action().equals("DELETE")) {
                    deleteEventFound = true;
                    break;
                }
            }

            assertThat(deleteEventFound).as("Should find DELETE event for email: " + uniqueEmail).isTrue();
        }
    }

    @Test
    void shouldGetAllUsers() {
        CreateUserRequest request1 = new CreateUserRequest("User One", "user1@example.com", 25);
        CreateUserRequest request2 = new CreateUserRequest("User Two", "user2@example.com", 30);

        userService.createUser(request1);
        userService.createUser(request2);

        List<UserDTO> users = userService.getAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserDTO::email)
                .containsExactly("user1@example.com", "user2@example.com");
        assertThat(users).extracting(UserDTO::name)
                .containsExactly("User One", "User Two");
    }

    @Test
    void shouldGetUserById() {
        CreateUserRequest request = new CreateUserRequest("Test User", "test@example.com", 25);
        UserDTO createdUser = userService.createUser(request);

        UserDTO foundUser = userService.getUserById(createdUser.id());

        assertThat(foundUser.id()).isEqualTo(createdUser.id());
        assertThat(foundUser.name()).isEqualTo("Test User");
        assertThat(foundUser.email()).isEqualTo("test@example.com");
        assertThat(foundUser.age()).isEqualTo(25);
        assertThat(foundUser.createdAt()).isNotNull();
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenGettingNonExistentUser() {
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User with id 999 not found");
    }

    @Test
    void shouldUpdateUserName() {
        CreateUserRequest createRequest = new CreateUserRequest("Old Name", "test@example.com", 25);
        UserDTO createdUser = userService.createUser(createRequest);
        UpdateUserRequest updateRequest = new UpdateUserRequest("New Name", null, null);

        UserDTO updatedUser = userService.updateUser(createdUser.id(), updateRequest);

        assertThat(updatedUser.id()).isEqualTo(createdUser.id());
        assertThat(updatedUser.name()).isEqualTo("New Name");
        assertThat(updatedUser.email()).isEqualTo("test@example.com");
        assertThat(updatedUser.age()).isEqualTo(25);
    }

    @Test
    void shouldUpdateUserEmail() {
        CreateUserRequest createRequest = new CreateUserRequest("Test User", "old@example.com", 25);
        UserDTO createdUser = userService.createUser(createRequest);
        UpdateUserRequest updateRequest = new UpdateUserRequest(null, "new@example.com", null);

        UserDTO updatedUser = userService.updateUser(createdUser.id(), updateRequest);

        assertThat(updatedUser.id()).isEqualTo(createdUser.id());
        assertThat(updatedUser.name()).isEqualTo("Test User");
        assertThat(updatedUser.email()).isEqualTo("new@example.com");
        assertThat(updatedUser.age()).isEqualTo(25);
    }

    @Test
    void shouldUpdateUserAge() {
        CreateUserRequest createRequest = new CreateUserRequest("Test User", "test@example.com", 25);
        UserDTO createdUser = userService.createUser(createRequest);
        UpdateUserRequest updateRequest = new UpdateUserRequest(null, null, 30);

        UserDTO updatedUser = userService.updateUser(createdUser.id(), updateRequest);

        assertThat(updatedUser.id()).isEqualTo(createdUser.id());
        assertThat(updatedUser.name()).isEqualTo("Test User");
        assertThat(updatedUser.email()).isEqualTo("test@example.com");
        assertThat(updatedUser.age()).isEqualTo(30);
    }

    @Test
    void shouldUpdateAllUserFields() {
        CreateUserRequest createRequest = new CreateUserRequest("Old Name", "old@example.com", 25);
        UserDTO createdUser = userService.createUser(createRequest);
        UpdateUserRequest updateRequest = new UpdateUserRequest("New Name", "new@example.com", 30);

        UserDTO updatedUser = userService.updateUser(createdUser.id(), updateRequest);

        assertThat(updatedUser.id()).isEqualTo(createdUser.id());
        assertThat(updatedUser.name()).isEqualTo("New Name");
        assertThat(updatedUser.email()).isEqualTo("new@example.com");
        assertThat(updatedUser.age()).isEqualTo(30);
    }

    @Test
    void shouldThrowEmailAlreadyExistsExceptionWhenUpdatingToExistingEmail() {
        CreateUserRequest request1 = new CreateUserRequest("User One", "user1@example.com", 25);
        CreateUserRequest request2 = new CreateUserRequest("User Two", "user2@example.com", 30);

        UserDTO user1 = userService.createUser(request1);
        userService.createUser(request2);

        UpdateUserRequest updateRequest = new UpdateUserRequest(null, "user2@example.com", null);

        assertThatThrownBy(() -> userService.updateUser(user1.id(), updateRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email 'user2@example.com' already exists");
    }

    @Test
    void shouldNotThrowExceptionWhenUpdatingToSameEmail() {
        CreateUserRequest createRequest = new CreateUserRequest("Test User", "test@example.com", 25);
        UserDTO createdUser = userService.createUser(createRequest);
        UpdateUserRequest updateRequest = new UpdateUserRequest(null, "test@example.com", null);

        UserDTO updatedUser = userService.updateUser(createdUser.id(), updateRequest);

        assertThat(updatedUser.email()).isEqualTo("test@example.com");
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUpdatingNonExistentUser() {
        UpdateUserRequest updateRequest = new UpdateUserRequest("New Name", "new@example.com", 30);

        assertThatThrownBy(() -> userService.updateUser(999L, updateRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User with id 999 not found");
    }

    @Test
    void shouldThrowEmailAlreadyExistsExceptionWhenCreatingUserWithExistingEmail() {
        CreateUserRequest request1 = new CreateUserRequest("User One", "same@example.com", 25);
        userService.createUser(request1);

        CreateUserRequest request2 = new CreateUserRequest("User Two", "same@example.com", 30);

        assertThatThrownBy(() -> userService.createUser(request2))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email 'same@example.com' already exists");
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenDeletingNonExistentUser() {
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User with id 999 not found");
    }

    @Test
    void shouldIgnoreBlankNameWhenUpdating() {
        CreateUserRequest createRequest = new CreateUserRequest("Original Name", "test@example.com", 25);
        UserDTO createdUser = userService.createUser(createRequest);
        UpdateUserRequest updateRequest = new UpdateUserRequest("   ", null, null);

        UserDTO updatedUser = userService.updateUser(createdUser.id(), updateRequest);

        assertThat(updatedUser.name()).isEqualTo("Original Name");
    }

    @Test
    void shouldIgnoreBlankEmailWhenUpdating() {
        CreateUserRequest createRequest = new CreateUserRequest("Test User", "original@example.com", 25);
        UserDTO createdUser = userService.createUser(createRequest);
        UpdateUserRequest updateRequest = new UpdateUserRequest(null, "   ", null);

        UserDTO updatedUser = userService.updateUser(createdUser.id(), updateRequest);

        assertThat(updatedUser.email()).isEqualTo("original@example.com");
    }

    @Test
    void shouldTrimNameAndEmailWhenCreatingUser() {
        CreateUserRequest request = new CreateUserRequest("Test User", "test@example.com", 25);

        UserDTO user = userService.createUser(request);

        assertThat(user.name()).isEqualTo("Test User");
        assertThat(user.email()).isEqualTo("test@example.com");
    }

    @Test
    void shouldTrimNameAndEmailWhenUpdatingUser() {
        CreateUserRequest createRequest = new CreateUserRequest("Old Name", "old@example.com", 25);
        UserDTO createdUser = userService.createUser(createRequest);
        UpdateUserRequest updateRequest = new UpdateUserRequest("  New Name  ", "  new@example.com  ", 30);

        UserDTO updatedUser = userService.updateUser(createdUser.id(), updateRequest);

        assertThat(updatedUser.name()).isEqualTo("New Name");
        assertThat(updatedUser.email()).isEqualTo("new@example.com");
    }
}
