package com.example.financetracker.auth.api.dto;

public record AuthResponse(
        String accessToken,
        UserResponse user
) {
}
