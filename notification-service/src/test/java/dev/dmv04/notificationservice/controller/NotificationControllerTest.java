package dev.dmv04.notificationservice.controller;

import dev.dmv04.notificationservice.dto.UserEvent;
import dev.dmv04.notificationservice.service.EmailNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailNotificationService emailNotificationService;

    @Test
    void shouldSendNotification() throws Exception {
        UserEvent event = new UserEvent("test@mail.ru", "CREATE");

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links").exists())
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.self.href").exists());

        verify(emailNotificationService).sendNotification(event);
    }

    @Test
    void shouldSendNotificationWithCorrectHateoasLinks() throws Exception {
        UserEvent event = new UserEvent("verchenko.d.s@mail.ru", "DELETE");

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification for DELETE action sent to verchenko.d.s@mail.ru"))
                .andExpect(jsonPath("$._links").isMap())
                .andExpect(jsonPath("$._links.self").isMap())
                .andExpect(jsonPath("$._links.self.href").isString())
                .andExpect(jsonPath("$._links").value(org.hamcrest.Matchers.hasKey("self")))
                .andExpect(jsonPath("$._links").value(org.hamcrest.Matchers.aMapWithSize(1)));

        verify(emailNotificationService).sendNotification(event);
    }


    @Test
    void shouldHandleServiceException() throws Exception {
        UserEvent event = new UserEvent("test@mail.ru", "CREATE");

        doThrow(new RuntimeException("Service error"))
                .when(emailNotificationService).sendNotification(event);

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().is5xxServerError());
    }
}
