package dev.dmv04.notificationservice.service;

import dev.dmv04.notificationservice.dto.UserEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    @Test
    void shouldSendNotificationForCreateEvent() {
        UserEvent event = new UserEvent("test@mail.ru", "CREATE");

        emailNotificationService.sendNotification(event);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSendNotificationForDeleteEvent() {
        UserEvent event = new UserEvent("test@mail.ru", "DELETE");

        emailNotificationService.sendNotification(event);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSetCorrectEmailPropertiesForCreateEvent() {
        UserEvent event = new UserEvent("test@mail.ru", "CREATE");

        emailNotificationService.sendNotification(event);

        verify(mailSender).send(argThat((SimpleMailMessage message) ->
                message.getTo()[0].equals("test@mail.ru") &&
                        message.getSubject().equals("Уведомление о вашем аккаунте") &&
                        message.getText().contains("был успешно создан")
        ));
    }

    @Test
    void shouldSetCorrectEmailPropertiesForDeleteEvent() {
        UserEvent event = new UserEvent("test@mail.ru", "DELETE");

        emailNotificationService.sendNotification(event);

        verify(mailSender).send(argThat((SimpleMailMessage message) ->
                message.getTo()[0].equals("test@mail.ru") &&
                        message.getSubject().equals("Уведомление о вашем аккаунте") &&
                        message.getText().contains("был удалён")
        ));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForNullAction() {
        UserEvent event = new UserEvent("test@mail.ru", null);

        assertThatThrownBy(() -> emailNotificationService.sendNotification(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown event: null");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForEmptyAction() {
        UserEvent event = new UserEvent("test@mail.ru", "");

        assertThatThrownBy(() -> emailNotificationService.sendNotification(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown event: ");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForBlankAction() {
        UserEvent event = new UserEvent("test@mail.ru", "   ");

        assertThatThrownBy(() -> emailNotificationService.sendNotification(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown event:    ");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
