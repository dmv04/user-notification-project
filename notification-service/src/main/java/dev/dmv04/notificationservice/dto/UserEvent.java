package dev.dmv04.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserEvent(String email, String action) {
    public static final String CREATE = "CREATE";
    public static final String DELETE = "DELETE";

    @JsonCreator
    public UserEvent(@JsonProperty("email") String email, @JsonProperty("action") String action) {
        this.email = email;
        this.action = action;
    }
}
