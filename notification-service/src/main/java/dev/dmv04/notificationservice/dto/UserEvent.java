package dev.dmv04.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserEvent(
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,

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
