package com.example.financetracker.finance.api.error;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException(UUID userId) {
        super(HttpStatus.NOT_FOUND, "User with id '%s' not found".formatted(userId));
    }
}
