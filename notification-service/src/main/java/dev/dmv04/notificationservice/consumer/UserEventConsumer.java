package dev.dmv04.notificationservice.consumer;

import dev.dmv04.notificationservice.dto.UserEvent;
import dev.dmv04.notificationservice.service.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);
    private final EmailNotificationService emailNotificationService;

    public UserEventConsumer(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @KafkaListener(topics = "user-events", groupId = "notification-group")
    public void consume(UserEvent event) {
        try {
            log.info("Received event: {} for email {}", event.action(), event.email());
            emailNotificationService.sendNotification(event);
        } catch (Exception e) {
            log.error("Failed to process user event", e);
        }
    }
}
