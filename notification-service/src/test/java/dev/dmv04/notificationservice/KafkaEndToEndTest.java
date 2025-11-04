package dev.dmv04.notificationservice;

import dev.dmv04.notificationservice.dto.UserEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import jakarta.mail.internet.MimeMessage;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class KafkaEndToEndTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.3.0")
    );

    @Autowired
    private KafkaTemplate<String, UserEvent> kafkaTemplate;

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser("test@mail.ru", "password"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    void shouldReceiveCreateEventAndSendEmail() throws Exception {
        String userEmail = "test@mail.ru";
        UserEvent event = new UserEvent(userEmail, "CREATE");

        kafkaTemplate.send("user-events", event).get(5, TimeUnit.SECONDS);

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    MimeMessage[] messages = greenMail.getReceivedMessages();

                    assertThat(messages).hasSize(1);

                    MimeMessage message = messages[0];

                    assertThat(message.getAllRecipients()[0].toString()).isEqualTo(userEmail);

                    String subject = message.getSubject();
                    assertThat(subject).isEqualTo("Уведомление о вашем аккаунте");

                    String content = message.getContent().toString();

                    assertThat(content).contains("Здравствуйте!");
                    assertThat(content).contains("Ваш аккаунт на сайте example.com был успешно создан");
                });
    }

    @Test
    void shouldReceiveDeleteEventAndSendEmail() throws Exception {
        String userEmail = "user@mail.ru";
        UserEvent event = new UserEvent(userEmail, "DELETE");

        kafkaTemplate.send("user-events", event).get(5, TimeUnit.SECONDS);

        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    MimeMessage[] messages = greenMail.getReceivedMessages();

                    assertThat(messages).hasSize(1);

                    MimeMessage message = messages[0];

                    assertThat(message.getAllRecipients()[0].toString()).isEqualTo(userEmail);

                    String subject = message.getSubject();
                    assertThat(subject).isEqualTo("Уведомление о вашем аккаунте");

                    String content = message.getContent().toString();

                    assertThat(content).contains("Ваш аккаунт был удалён");
                });
    }
}
