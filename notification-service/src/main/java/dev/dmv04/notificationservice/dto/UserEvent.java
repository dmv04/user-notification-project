package dev.dmv04.notificationservice.dto;

public record UserEvent(String email, String action) {
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_DELETE = "DELETE";
}
