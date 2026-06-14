package com.example.financetracker.finance.api.error;

import org.springframework.http.HttpStatus;

public class InvalidOperationFilterException extends ApiException {

    public InvalidOperationFilterException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
