package com.example.financetracker.finance.api.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> details;

    protected ApiException(HttpStatus status, String message) {
        this(status, message, Map.of());
    }

    protected ApiException(HttpStatus status, String message, Map<String, String> details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
