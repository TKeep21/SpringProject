package com.example.financetracker.auth.service;

import com.example.financetracker.auth.api.dto.AuthResponse;
import com.example.financetracker.auth.api.dto.LoginRequest;
import com.example.financetracker.auth.api.dto.RegisterRequest;
import com.example.financetracker.auth.api.dto.UserResponse;
import com.example.financetracker.auth.api.error.DuplicateEmailException;
import com.example.financetracker.auth.api.error.InvalidCredentialsException;
import com.example.financetracker.auth.api.error.UserNotFoundException;
import com.example.financetracker.auth.security.AuthenticatedUser;
import com.example.financetracker.auth.security.JwtService;
import com.example.financetracker.auth.user.User;
import com.example.financetracker.auth.user.UserRepository;
import com.example.financetracker.auth.user.UserRole;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setRole(UserRole.ROLE_USER);

        User savedUser = userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(savedUser), toUserResponse(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new AuthResponse(jwtService.generateToken(user), toUserResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        AuthenticatedUser principal = getAuthenticatedUser();
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));
        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id '%s' not found".formatted(id)));
        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("User with email '%s' not found".formatted(normalizedEmail)));
        return toUserResponse(user);
    }

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }
        throw new UserNotFoundException("Authenticated user not found");
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
