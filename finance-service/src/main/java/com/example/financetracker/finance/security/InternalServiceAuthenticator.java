package com.example.financetracker.finance.security;

import com.example.financetracker.finance.api.error.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class InternalServiceAuthenticator {

    public static final String HEADER_NAME = "X-Internal-Token";

    private final InternalServiceProperties properties;

    public InternalServiceAuthenticator(InternalServiceProperties properties) {
        this.properties = properties;
    }

    public void requireValidToken(String token) {
        if (properties.serviceToken() == null
                || properties.serviceToken().isBlank()
                || token == null
                || !properties.serviceToken().equals(token)) {
            throw new AccessDeniedException("Invalid internal service token");
        }
    }
}
