package dev.dmv04.userservice.dto;

import java.time.LocalDateTime;

import org.springframework.hateoas.RepresentationModel;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    public String name() {
        return name;
    }

    @JsonProperty("email")
    public String email() {
        return email;
    }

    @JsonProperty("age")
    public Integer age() {
        return age;
    }

    @JsonProperty("id")
    public Long id() {
        return id;
    }

    @JsonProperty("createdAt")
    public LocalDateTime createdAt() {
        return createdAt;
    }
}
