package dev.dmv04.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Событие пользователя для отправки уведомления")
public record UserEvent(
        @Schema(description = "Email адрес получателя", example = "verchenko.d.s@mail.ru", format = "email")
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,

        @Schema(description = "Действие для уведомления", example = "CREATE", allowableValues = {"CREATE", "DELETE"})
        @NotBlank(message = "Action is required")
        @Pattern(regexp = "CREATE|DELETE", message = "Action must be either 'CREATE' or 'DELETE'")
        String action
) {
    public static final String CREATE = "CREATE";
    public static final String DELETE = "DELETE";

    @JsonCreator
    public UserEvent(@JsonProperty("email") String email, @JsonProperty("action") String action) {
        this.email = email;
        this.action = action;
    }
}
