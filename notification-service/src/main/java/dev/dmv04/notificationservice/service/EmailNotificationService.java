package dev.dmv04.notificationservice.service;
import dev.dmv04.notificationservice.dto.UserEvent;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendNotification(UserEvent event) {
        String subject = "Уведомление о вашем аккаунте";
        String text;
        if (UserEvent.ACTION_CREATE.equals(event.action())) {
            text = "Здравствуйте! Ваш аккаунт на сайте example.com был успешно создан.";
        } else if (UserEvent.ACTION_DELETE.equals(event.action())) {
            text = "Здравствуйте! Ваш аккаунт был удалён.";
        } else {
            throw new IllegalArgumentException("Unknown event: " + event.action());
        }
        sendEmail(event.email(), subject, text);
    }

    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
