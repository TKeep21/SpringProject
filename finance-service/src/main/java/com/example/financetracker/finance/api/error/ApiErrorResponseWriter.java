package com.example.financetracker.finance.api.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorResponseWriter {

    private final ApiErrorResponseFactory responseFactory;
    private final ObjectMapper objectMapper;

    public ApiErrorResponseWriter(ApiErrorResponseFactory responseFactory, ObjectMapper objectMapper) {
        this.responseFactory = responseFactory;
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, String message, String path) throws IOException {
        write(response, status, message, path, Map.of());
    }

    public void write(
            HttpServletResponse response,
            HttpStatus status,
            String message,
            String path,
            Map<String, String> details
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), responseFactory.create(status, message, path, details));
    }
}
