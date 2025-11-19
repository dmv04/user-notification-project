package dev.dmv04.userservice.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.RepresentationModel;
import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "DTO пользователя")
public class UserDTO extends RepresentationModel<UserDTO> {
    private final Long id;
    private final String name;
    private final String email;
    private final Integer age;
    private final LocalDateTime createdAt;

    public UserDTO(Long id, String name, String email, Integer age, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.createdAt = createdAt;
    }

    @JsonProperty("name")
    @Schema(description = "Имя пользователя", example = "Иван Иванов")
    public String name() {
        return name;
    }

    @JsonProperty("email")
    @Schema(description = "Email пользователя", example = "ivan@example.com", format = "email")

    public String email() {
        return email;
    }

    @JsonProperty("age")
    @Schema(description = "Возраст пользователя", example = "30")
    public Integer age() {
        return age;
    }

    @JsonProperty("id")
    @Schema(description = "ID пользователя", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    public Long id() {
        return id;
    }

    @JsonProperty("createdAt")
    @Schema(description = "Дата создания", example = "2024-01-15T10:30:00",
            format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    public LocalDateTime createdAt() {
        return createdAt;
    }
}
