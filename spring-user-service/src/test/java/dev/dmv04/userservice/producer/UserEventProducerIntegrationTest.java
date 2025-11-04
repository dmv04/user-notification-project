package dev.dmv04.userservice.producer;

import dev.dmv04.userservice.dto.UserEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
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
class UserEventProducerIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.3.0")
    );

    @Autowired
    private UserEventProducer userEventProducer;

    private Consumer<String, UserEvent> consumer;

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
        consumer = createConsumerForUserEvent(kafka, "user-events");
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void shouldSendUserEventToKafka() {
        UserEvent event = new UserEvent("test@mail.ru", "CREATE");

        userEventProducer.sendUserEvent(event);

        ConsumerRecords<String, UserEvent> records =
                KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

        assertThat(records).isNotEmpty();

        var record = records.iterator().next();
        assertThat(record.value().email()).isEqualTo("test@mail.ru");
        assertThat(record.value().action()).isEqualTo("CREATE");
    }

    @Test
    void shouldSendUserEventWithEmailAndActionToKafka() {
        userEventProducer.sendUserEvent("test@mail.ru", "CREATE");

        ConsumerRecords<String, UserEvent> records =
                KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

        assertThat(records).isNotEmpty();

        var record = records.iterator().next();
        assertThat(record.value().email()).isEqualTo("test@mail.ru");
        assertThat(record.value().action()).isEqualTo("CREATE");
    }
}
