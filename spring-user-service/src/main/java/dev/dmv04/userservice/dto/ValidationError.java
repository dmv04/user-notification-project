package dev.dmv04.userservice.dto;

public record ValidationError(
        String field,
        String message,
        String rejectedValue
) {}
