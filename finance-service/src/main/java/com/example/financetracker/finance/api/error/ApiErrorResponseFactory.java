package com.example.financetracker.finance.api.error;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorResponseFactory {

    public ApiErrorResponse create(HttpStatus status, String message, String path) {
        return create(status, message, path, Map.of());
    }

    public ApiErrorResponse create(HttpStatus status, String message, String path, Map<String, String> details) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                details
        );
    }
}
