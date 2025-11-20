package dev.dmv04.notificationservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.dmv04.notificationservice.controller.NotificationController;
import dev.dmv04.notificationservice.dto.UserEvent;
import dev.dmv04.notificationservice.exception.EmailSendingException;
import dev.dmv04.notificationservice.exception.InvalidUserEventException;
import dev.dmv04.notificationservice.service.EmailNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false"
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailNotificationService emailNotificationService;

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        UserEvent invalidEvent = new UserEvent("not-an-email", "CREATE");

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEvent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").exists());
    }

    @Test
    void shouldReturn400WhenActionIsInvalid() throws Exception {
        UserEvent invalidEvent = new UserEvent("user@example.com", "UPDATE");

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEvent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details.action").value("Action must be either 'CREATE' or 'DELETE'"));
    }

    @Test
    void shouldReturn400WhenInvalidUserEventExceptionIsThrown() throws Exception {
        UserEvent event = new UserEvent("user@example.com", "CREATE");
        doThrow(new InvalidUserEventException("Manual validation failed"))
                .when(emailNotificationService).sendNotification(event);

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"))
                .andExpect(jsonPath("$.message").value("Manual validation failed"));
    }

    @Test
    void shouldReturn503WhenEmailSendingFails() throws Exception {
        UserEvent event = new UserEvent("user@example.com", "CREATE");
        doThrow(new EmailSendingException("SMTP connection failed", new RuntimeException()))
                .when(emailNotificationService).sendNotification(event);

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Email delivery failed"))
                .andExpect(jsonPath("$.message").value("SMTP connection failed"));
    }

    @Test
    void shouldReturn500WithUnknownErrorWhenExceptionHasNoMessage() throws Exception {
        UserEvent event = new UserEvent("user@example.com", "CREATE");
        doThrow(new RuntimeException())
                .when(emailNotificationService).sendNotification(event);

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"))
                .andExpect(jsonPath("$.message").value("Unknown error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}