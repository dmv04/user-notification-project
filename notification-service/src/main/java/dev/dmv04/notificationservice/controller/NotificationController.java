package dev.dmv04.notificationservice.controller;

import dev.dmv04.notificationservice.dto.UserEvent;
import dev.dmv04.notificationservice.service.EmailNotificationService;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final EmailNotificationService emailNotificationService;

    public NotificationController(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<EntityModel<Map<String, Object>>> sendNotification(@Valid @RequestBody UserEvent event) {
        emailNotificationService.sendNotification(event);

        Map<String, Object> response = new HashMap<>();
        response.put("message", String.format("Notification for %s action sent to %s", event.action(), event.email()));
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "email-notification");

        EntityModel<Map<String, Object>> resource = EntityModel.of(response);

        resource.add(linkTo(methodOn(NotificationController.class).sendNotification(event)).withSelfRel());

        return ResponseEntity.ok(resource);
    }
}
