package com.example.financetracker.finance.client;

import com.example.financetracker.finance.api.error.AccessDeniedException;
import com.example.financetracker.finance.api.error.AuthenticatedUserNotAvailableException;
import com.example.financetracker.finance.api.error.ExternalServiceException;
import com.example.financetracker.finance.api.error.UserNotFoundException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthUsersClient {

    private final RestClient restClient;

    public AuthUsersClient(RestClient authServiceRestClient) {
        this.restClient = authServiceRestClient;
    }

    public void requireUserExists(UUID userId) {
        try {
            restClient.get()
                    .uri("/internal/users/{id}", userId)
                    .header(HttpHeaders.AUTHORIZATION, currentAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResourceAccessException exception) {
            throw new ExternalServiceException("Auth service is temporarily unavailable");
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new UserNotFoundException(userId);
            }
            if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED
                    || exception.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new AccessDeniedException("Cannot verify user access in auth service");
            }
            if (exception.getStatusCode().is5xxServerError()) {
                throw new ExternalServiceException("Auth service is temporarily unavailable");
            }
            throw exception;
        }
    }

    private String currentAuthorizationHeader() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String header = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && !header.isBlank()) {
                return header;
            }
        }
        throw new AuthenticatedUserNotAvailableException();
    }
}
