package com.example.financetracker.finance.api.error;

import org.springframework.http.HttpStatus;

public class AuthenticatedUserNotAvailableException extends ApiException {

    public AuthenticatedUserNotAvailableException() {
        super(HttpStatus.UNAUTHORIZED, "Authenticated user is not available");
    }
}
