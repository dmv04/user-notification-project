package dev.dmv04.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;

@Schema(description = "Запрос на обновление пользователя")
public record UpdateUserRequest(
        @Schema(description = "Имя пользователя", example = "Иван Иванов")
        String name,

        @Schema(description = "Email пользователя", example = "ivan@example.com", format = "email")
        @Email
        String email,

        @Schema(description = "Возраст пользователя", example = "26", minimum = "1")
        @Min(1)
        Integer age
) {}
