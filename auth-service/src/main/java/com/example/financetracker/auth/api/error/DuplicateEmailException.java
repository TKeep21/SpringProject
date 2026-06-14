package com.example.financetracker.auth.api.error;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("User with email '%s' already exists".formatted(email));
    }
}
