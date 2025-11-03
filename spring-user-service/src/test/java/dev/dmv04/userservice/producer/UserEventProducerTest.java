package dev.dmv04.userservice.producer;

import dev.dmv04.userservice.dto.UserEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserEventProducerTest {

    @Mock
    private KafkaTemplate<String, UserEvent> kafkaTemplate;

    @InjectMocks
    private UserEventProducer userEventProducer;

    @Test
    void shouldSendUserEventSuccessfully() {
        UserEvent event = new UserEvent("test@mail.ru", "CREATE");
        CompletableFuture<SendResult<String, UserEvent>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(eq("user-events"), any(UserEvent.class)))
                .thenReturn(future);

        userEventProducer.sendUserEvent(event);

        verify(kafkaTemplate, timeout(1000)).send("user-events", event);
    }

    @Test
    void shouldSendUserEventWithEmailAndAction() {
        String email = "test@mail.ru";
        String action = "CREATE";
        CompletableFuture<SendResult<String, UserEvent>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(eq("user-events"), any(UserEvent.class)))
                .thenReturn(future);

        userEventProducer.sendUserEvent(email, action);

        verify(kafkaTemplate, timeout(1000)).send(eq("user-events"),
                argThat(event ->
                        event.email().equals(email) &&
                                event.action().equals(action)
                ));
    }

    @Test
    void shouldHandleKafkaSendFailure() {
        UserEvent event = new UserEvent("test@mail.ru", "CREATE");
        CompletableFuture<SendResult<String, UserEvent>> future =
                new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));

        when(kafkaTemplate.send(eq("user-events"), any(UserEvent.class)))
                .thenReturn(future);

        userEventProducer.sendUserEvent(event);

        verify(kafkaTemplate, timeout(1000)).send("user-events", event);
    }

    @Test
    void shouldLogSuccessWhenMessageSent() {
        UserEvent event = new UserEvent("test@mail.ru", "CREATE");
        SendResult<String, UserEvent> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, UserEvent>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(eq("user-events"), any(UserEvent.class)))
                .thenReturn(future);

        userEventProducer.sendUserEvent(event);

        verify(kafkaTemplate, timeout(1000)).send("user-events", event);
    }

    @Test
    void shouldSendMultipleEvents() {
        UserEvent event1 = new UserEvent("user1@mail.ru", "CREATE");
        UserEvent event2 = new UserEvent("user2@mail.ru", "DELETE");
        CompletableFuture<SendResult<String, UserEvent>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(eq("user-events"), any(UserEvent.class)))
                .thenReturn(future);

        userEventProducer.sendUserEvent(event1);
        userEventProducer.sendUserEvent(event2);

        verify(kafkaTemplate, times(2)).send(eq("user-events"), any(UserEvent.class));
    }
}
