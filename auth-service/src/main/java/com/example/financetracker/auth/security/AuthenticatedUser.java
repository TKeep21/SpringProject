package com.example.financetracker.auth.security;

import com.example.financetracker.auth.user.UserRole;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email,
        UserRole role
) {
}
