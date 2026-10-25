package com.taskmanager.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request.email(), request.password())
                .map(user -> new UserResponse(user.id(), user.email(), user.createdAt()));
    }

    @PostMapping("/login")
    public Mono<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return service.login(request.email(), request.password())
                .map(token -> new TokenResponse(token.value(), "Bearer", token.expiresIn()));
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotNull @Size(min = 8, max = 72) @MaxBytes(72) String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record UserResponse(UUID id, String email, Instant createdAt) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }
}
