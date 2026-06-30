package com.example.financetracker.finance.client;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email
) {
}
