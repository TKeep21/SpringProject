package com.example.financetracker.finance.api.error;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends ApiException {

    public AccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
