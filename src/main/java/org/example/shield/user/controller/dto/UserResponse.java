package org.example.shield.user.controller.dto;

import org.example.shield.user.domain.User;

import java.util.UUID;

public record UserResponse(
        UUID userId,
        String email,
        String name,
        String role,
        String provider,
        String profileImageUrl,
        String phone
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getProvider(),
                user.getProfileImageUrl(),
                user.getPhone()
        );
    }
}
