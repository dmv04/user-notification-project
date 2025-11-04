package dev.dmv04.userservice.service;

import dev.dmv04.userservice.dto.CreateUserRequest;
import dev.dmv04.userservice.dto.UserEvent;
import dev.dmv04.userservice.repository.UserRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static dev.dmv04.userservice.util.KafkaTestConsumerUtil.createConsumerForUserEvent;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class UserServiceKafkaIntegrationTest {

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
    void cleanDatabase() {
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
}
