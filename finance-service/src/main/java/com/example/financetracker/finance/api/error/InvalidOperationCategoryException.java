package com.example.financetracker.finance.api.error;

import org.springframework.http.HttpStatus;

public class InvalidOperationCategoryException extends ApiException {

    public InvalidOperationCategoryException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
