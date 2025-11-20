package dev.dmv04.notificationservice.consumer;

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
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.config.location=classpath:application-test.yml",
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.cloud.config.enabled=false"
})
@EmbeddedKafka(topics = {"user-events"}, partitions = 1)
@DirtiesContext
class UserEventConsumerIntegrationTest {

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
    void shouldProcessCreateEventAndSendWelcomeEmail() {
        String testEmail = "newuser@example.com";
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
                    assertThat(message.getAllRecipients()[0].toString()).isEqualTo(testEmail);
                });
    }

    @Test
    void shouldProcessDeleteEventAndSendGoodbyeEmail() {
        String testEmail = "deleteduser@example.com";
        UserEvent event = new UserEvent(testEmail, UserEvent.DELETE);

        kafkaTemplate.send("user-events", event);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    MimeMessage[] messages = greenMail.getReceivedMessages();
                    assertThat(messages).hasSize(1);

                    MimeMessage message = messages[0];
                    assertThat(message.getSubject()).isEqualTo("Уведомление о вашем аккаунте");
                    assertThat(message.getContent().toString())
                            .contains("Ваш аккаунт был удалён");
                    assertThat(message.getAllRecipients()[0].toString()).isEqualTo(testEmail);
                });
    }

    @Test
    void shouldHandleMultipleEventsConcurrently() {
        UserEvent event1 = new UserEvent("user1@example.com", UserEvent.CREATE);
        UserEvent event2 = new UserEvent("user2@example.com", UserEvent.DELETE);
        UserEvent event3 = new UserEvent("user3@example.com", UserEvent.CREATE);

        kafkaTemplate.send("user-events", event1);
        kafkaTemplate.send("user-events", event2);
        kafkaTemplate.send("user-events", event3);

        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    MimeMessage[] messages = greenMail.getReceivedMessages();
                    assertThat(messages).hasSize(3);

                    String[] recipients = new String[3];
                    for (int i = 0; i < messages.length; i++) {
                        recipients[i] = messages[i].getAllRecipients()[0].toString();
                    }

                    assertThat(recipients).containsExactlyInAnyOrder(
                            "user1@example.com",
                            "user2@example.com",
                            "user3@example.com"
                    );
                });
    }

    @Test
    void shouldProcessEventsWithDifferentEmailFormats() {
        String[] testEmails = {
                "simple@example.com",
                "user.with.dots@example.com",
                "user+tag@example.com",
                "user_name@example.com"
        };

        for (String email : testEmails) {
            kafkaTemplate.send("user-events", new UserEvent(email, UserEvent.CREATE));
        }

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    MimeMessage[] messages = greenMail.getReceivedMessages();
                    assertThat(messages).hasSize(testEmails.length);

                    String[] recipients = new String[messages.length];
                    for (int i = 0; i < messages.length; i++) {
                        recipients[i] = messages[i].getAllRecipients()[0].toString();
                    }

                    assertThat(recipients).containsExactlyInAnyOrder(testEmails);
                });
    }

    @Test
    void shouldHandleEmptyOrNullEventsGracefully() {
        UserEvent emptyEmailEvent = new UserEvent("", UserEvent.CREATE);
        UserEvent nullEmailEvent = new UserEvent(null, UserEvent.DELETE);

        kafkaTemplate.send("user-events", emptyEmailEvent);
        kafkaTemplate.send("user-events", nullEmailEvent);

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    MimeMessage[] messages = greenMail.getReceivedMessages();
                    assertThat(messages).hasSize(0);
                });
    }
}