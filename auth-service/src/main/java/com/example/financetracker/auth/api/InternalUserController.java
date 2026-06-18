package com.example.financetracker.auth.api;

import com.example.financetracker.auth.api.dto.UserResponse;
import com.example.financetracker.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final AuthService authService;

    public InternalUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return authService.getUserById(id);
    }
}
