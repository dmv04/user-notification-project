package dev.dmv04.notificationservice.service;
import dev.dmv04.notificationservice.dto.UserEvent;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendNotification(UserEvent event) {
        String subject = "Уведомление о вашем аккаунте";
        String text;
        if (UserEvent.CREATE.equals(event.action())) {
            text = "Здравствуйте! Ваш аккаунт на сайте example.com был успешно создан.";
        } else if (UserEvent.DELETE.equals(event.action())) {
            text = "Здравствуйте! Ваш аккаунт был удалён.";
        } else {
            throw new IllegalArgumentException("Unknown event: " + event.action());
        }
        sendEmail(event.email(), subject, text);
    }

    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("verchenko.d.s@mail.ru");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
