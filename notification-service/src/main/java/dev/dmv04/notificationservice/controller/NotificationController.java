package dev.dmv04.notificationservice.controller;

import dev.dmv04.notificationservice.dto.UserEvent;
import dev.dmv04.notificationservice.service.EmailNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "notification-controller", description = "Отправка сообщений на почту")
public class NotificationController {

    private final EmailNotificationService emailNotificationService;

    public NotificationController(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @PostMapping("/send")
    @Operation(
            summary = "Отправляет сообщения на почту",
            description = "Отправляет сообщения на почту в зависимости от event"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content(
                            mediaType = "application/hal+json",
                            schema = @Schema(implementation = NotificationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибки валидации запроса (некорректный email, недопустимое значение action и т.д.)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Ошибка отправки письма (например, недоступен SMTP-сервер)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<EntityModel<Map<String, Object>>> sendNotification(
            @Parameter(description = "Данные для отправки уведомления")
            @Valid @RequestBody UserEvent event) {

        emailNotificationService.sendNotification(event);

        Map<String, Object> response = new HashMap<>();
        response.put("message", String.format("Notification for %s action sent to %s", event.action(), event.email()));
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "email-notification");

        EntityModel<Map<String, Object>> resource = EntityModel.of(response);
        resource.add(linkTo(methodOn(NotificationController.class).sendNotification(event)).withSelfRel());

        return ResponseEntity.ok(resource);
    }

    @Schema(name = "NotificationResponse")
    public static class NotificationResponse {
        @Schema(example = "Notification for CREATE action sent to verchenko.d.s@mail.ru")
        public String message;

        @Schema(example = "2025-11-11T14:30:00", format = "date-time")
        public String timestamp;

        @Schema(example = "email-notification")
        public String service;

        @Schema(additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        public Object additionalProperties;
    }

    @Schema(name = "ErrorResponse")
    public static class ErrorResponse {
        @Schema(example = "Validation failed")
        public String error;

        @Schema(example = "Action must be either 'CREATE' or 'DELETE'")
        public String message;

        @Schema(example = "2025-11-11T14:30:00", format = "date-time")
        public String timestamp;

        @Schema(additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
        public Object details;
    }
}
