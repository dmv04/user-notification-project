package dev.dmv04.notificationservice.consumer;

import dev.dmv04.notificationservice.dto.UserEvent;
import dev.dmv04.notificationservice.service.EmailNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventConsumerTest {

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private UserEventConsumer userEventConsumer;

    @Test
    void shouldProcessCreateEventSuccessfully() {
        UserEvent event = new UserEvent("test@mail.ru", "CREATE");

        userEventConsumer.consume(event);

        verify(emailNotificationService).sendNotification(event);
    }

    @Test
    void shouldProcessDeleteEventSuccessfully() {
        UserEvent event = new UserEvent("test@mail.ru", "DELETE");

        userEventConsumer.consume(event);

        verify(emailNotificationService).sendNotification(event);
    }

    @Test
    void shouldLogErrorWhenEmailServiceFails() {
        UserEvent event = new UserEvent("test@mail.ru", "CREATE");
        doThrow(new RuntimeException("Email service error"))
                .when(emailNotificationService).sendNotification(event);

        userEventConsumer.consume(event);

        verify(emailNotificationService).sendNotification(event);
    }
}
