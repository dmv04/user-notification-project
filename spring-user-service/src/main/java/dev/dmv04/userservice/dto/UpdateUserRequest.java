package dev.dmv04.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Positive;

public record UpdateUserRequest(
        String name,
        @Email String email,
        @Positive Integer age
) {}
