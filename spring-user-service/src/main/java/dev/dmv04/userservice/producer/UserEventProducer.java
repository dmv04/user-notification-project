package dev.dmv04.userservice.producer;

import dev.dmv04.userservice.dto.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class UserEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(UserEventProducer.class);

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private final String topic = "user-events";

    public UserEventProducer(KafkaTemplate<String, UserEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendUserEvent(String email, String eventType) {
        UserEvent event = new UserEvent(email, eventType);
        sendUserEvent(event);
    }

    public void sendUserEvent(UserEvent event) {
        try {
            CompletableFuture<SendResult<String, UserEvent>> future =
                    kafkaTemplate.send(topic, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Sent user event: {} for email: {}, offset: {}",
                            event.action(), event.email(), result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send user event: {} for email: {}",
                            event.action(), event.email(), ex);
                }
            });

        } catch (Exception ex) {
            logger.error("Error sending user event: {} for email: {}",
                    event.action(), event.email(), ex);
        }
    }
}
