package dev.dmv04.notificationservice.controller;

import dev.dmv04.notificationservice.dto.UserEvent;
import dev.dmv04.notificationservice.service.EmailNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final EmailNotificationService emailNotificationService;

    public NotificationController(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody UserEvent event) {
        emailNotificationService.sendNotification(event);
        return ResponseEntity.ok("Notification sent to " + event.email());
    }
}
