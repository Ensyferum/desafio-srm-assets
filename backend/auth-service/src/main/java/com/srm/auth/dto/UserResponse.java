package com.srm.auth.dto;

import com.srm.auth.domain.User;
import java.time.Instant;
import java.util.UUID;

/** Representação pública de um usuário. */
public record UserResponse(
        UUID id, String username, String fullName, String role, boolean active, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name(),
                user.isActive(),
                user.getCreatedAt());
    }
}
