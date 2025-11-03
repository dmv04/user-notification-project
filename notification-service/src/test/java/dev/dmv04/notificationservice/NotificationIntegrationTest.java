package dev.dmv04.notificationservice;

import com.icegreen.greenmail.store.FolderException;
import dev.dmv04.notificationservice.dto.UserEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.internet.MimeMessage;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        topics = {"user-events"},
        partitions = 1,
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        }
)
@DirtiesContext
class NotificationIntegrationTest {

    @Autowired
    private KafkaTemplate<String, UserEvent> kafkaTemplate;

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser("test@mail.ru", "password"));

    @BeforeEach
    void setUp() {
        try {
            greenMail.purgeEmailFromAllMailboxes();
        } catch (FolderException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        try {
            greenMail.purgeEmailFromAllMailboxes();
        } catch (FolderException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldReceiveCreateEventAndSendEmail() {
        String testEmail = "user1@example.com";
        UserEvent event = new UserEvent(testEmail, UserEvent.CREATE);

        kafkaTemplate.send("user-events", event);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    MimeMessage[] messages = greenMail.getReceivedMessages();
                    assertThat(messages).hasSize(1);

                    MimeMessage message = messages[0];
                    assertThat(message.getSubject()).isEqualTo("Уведомление о вашем аккаунте");
                    assertThat(message.getContent().toString())
                            .contains("аккаунт на сайте example.com был успешно создан");
                    assertThat(message.getAllRecipients()[0].toString()).hasToString(testEmail);
                });
    }

    @Test
    void shouldReceiveDeleteEventAndSendEmail() {
        String testEmail = "user2@example.com";
        UserEvent event = new UserEvent(testEmail, UserEvent.DELETE);

        kafkaTemplate.send("user-events", event);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    MimeMessage[] messages = greenMail.getReceivedMessages();
                    assertThat(messages).hasSize(1);
                    assertThat(messages[0].getContent().toString())
                            .contains("Ваш аккаунт был удалён");
                    assertThat(messages[0].getAllRecipients()[0].toString()).hasToString(testEmail);
                });
    }

    @Test
    void shouldSendMultipleEmailsForMultipleEvents() {
        UserEvent event1 = new UserEvent("user3@example.com", UserEvent.CREATE);
        UserEvent event2 = new UserEvent("user4@example.com", UserEvent.CREATE);

        kafkaTemplate.send("user-events", event1);
        kafkaTemplate.send("user-events", event2);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    MimeMessage[] messages = greenMail.getReceivedMessages();
                    assertThat(messages).hasSize(2);

                    String recipient1 = messages[0].getAllRecipients()[0].toString();
                    String recipient2 = messages[1].getAllRecipients()[0].toString();

                    assertThat(recipient1).isIn("user3@example.com", "user4@example.com");
                    assertThat(recipient2).isIn("user3@example.com", "user4@example.com");
                    assertThat(recipient1).isNotEqualTo(recipient2);
                });
    }
}
