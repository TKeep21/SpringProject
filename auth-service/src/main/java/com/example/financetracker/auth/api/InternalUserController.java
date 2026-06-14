package com.example.financetracker.auth.api;

import com.example.financetracker.auth.api.dto.UserResponse;
import com.example.financetracker.auth.security.InternalServiceAuthenticator;
import com.example.financetracker.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final AuthService authService;
    private final InternalServiceAuthenticator internalServiceAuthenticator;

    public InternalUserController(AuthService authService, InternalServiceAuthenticator internalServiceAuthenticator) {
        this.authService = authService;
        this.internalServiceAuthenticator = internalServiceAuthenticator;
    }

    @GetMapping("/{id}")
    public UserResponse getById(
            @PathVariable UUID id,
            @RequestHeader(value = InternalServiceAuthenticator.HEADER_NAME, required = false) String internalToken
    ) {
        internalServiceAuthenticator.requireValidToken(internalToken);
        return authService.getUserById(id);
    }
}
