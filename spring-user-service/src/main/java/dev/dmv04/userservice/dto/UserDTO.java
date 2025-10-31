package dev.dmv04.userservice.dto;

import java.time.LocalDateTime;

public record UserDTO(
        Long id,
        String name,
        String email,
        Integer age,
        LocalDateTime createdAt
) {
    public Long getId() {
        return id;
    }
}
