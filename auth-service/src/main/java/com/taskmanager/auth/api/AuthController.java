package com.taskmanager.auth.api;

import com.taskmanager.auth.dto.LoginRequest;
import com.taskmanager.auth.dto.Problem;
import com.taskmanager.auth.dto.RegisterRequest;
import com.taskmanager.auth.dto.TokenResponse;
import com.taskmanager.auth.dto.UserResponse;
import com.taskmanager.auth.service.AuthService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ApiResponse(responseCode = "201", description = "User registered; also returned for an existing email, which is not stored")
    @ApiResponse(responseCode = "400", description = "Invalid email or password, error per field; malformed body",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request.email(), request.password())
                .map(user -> new UserResponse(user.id(), user.email(), user.createdAt()));
    }

    @PostMapping("/login")
    @ApiResponse(responseCode = "200", description = "Bearer token valid for `expiresIn` seconds")
    @ApiResponse(responseCode = "400", description = "Missing fields, error per field; malformed body",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    @ApiResponse(responseCode = "401", description = "Wrong email or password (same response for both)",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = Problem.class)))
    public Mono<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return service.login(request.email(), request.password())
                .map(token -> new TokenResponse(token.value(), "Bearer", token.expiresIn()));
    }
}
