package dev.dmv04.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на создание пользователя")
public record CreateUserRequest(
        @Schema(description = "Имя пользователя", example = "Иван Иванов", minLength = 1)
        @NotBlank
        String name,

        @Schema(description = "Email пользователя", example = "ivan@example.com", format = "email")
        @Email
        @NotBlank
        String email,

        @Schema(description = "Возраст пользователя", example = "25", minimum = "1")
        @Min(1)
        Integer age
) {}
