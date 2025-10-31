package dev.dmv04.userservice.dto;

public record UserEvent(String email, String action) {
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_DELETE = "DELETE";
}
