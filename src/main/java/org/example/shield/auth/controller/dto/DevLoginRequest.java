package org.example.shield.auth.controller.dto;

public record DevLoginRequest(
        String email,
        String name,
        String role
) {}
