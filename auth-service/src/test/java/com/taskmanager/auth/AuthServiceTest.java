package com.taskmanager.auth;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final TokenIssuer tokenIssuer = mock(TokenIssuer.class);
    private final AuthService service = new AuthService(repository, passwordHasher, tokenIssuer);

    @Test
    void registerStoresNormalizedEmailAndHash() {
        when(passwordHasher.hash("password123")).thenReturn(Mono.just("hash"));
        when(repository.insertIfAbsent(any())).thenReturn(Mono.just(true));

        StepVerifier.create(service.register("  John@Example.COM ", "password123"))
                .assertNext(user -> {
                    assertThat(user.id()).isNotNull();
                    assertThat(user.email()).isEqualTo("john@example.com");
                    assertThat(user.passwordHash()).isEqualTo("hash");
                    assertThat(user.createdAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void registerWithExistingEmailStillReturnsAUser() {
        when(passwordHasher.hash("password123")).thenReturn(Mono.just("hash"));
        when(repository.insertIfAbsent(any())).thenReturn(Mono.just(false));

        StepVerifier.create(service.register("john@example.com", "password123"))
                .assertNext(user -> assertThat(user.email()).isEqualTo("john@example.com"))
                .verifyComplete();
    }

    @Test
    void loginWithValidCredentialsIssuesToken() {
        User user = user();
        AccessToken token = new AccessToken("jwt", 3600);
        when(repository.findByEmail("john@example.com")).thenReturn(Mono.just(user));
        when(passwordHasher.matches("password123", "hash")).thenReturn(Mono.just(true));
        when(tokenIssuer.issue(user.id())).thenReturn(token);

        StepVerifier.create(service.login("John@Example.com", "password123"))
                .expectNext(token)
                .verifyComplete();
    }

    @Test
    void loginWithWrongPasswordFails() {
        when(repository.findByEmail("john@example.com")).thenReturn(Mono.just(user()));
        when(passwordHasher.matches("wrong-password", "hash")).thenReturn(Mono.just(false));

        StepVerifier.create(service.login("john@example.com", "wrong-password"))
                .verifyError(InvalidCredentialsException.class);
        verify(passwordHasher, never()).compareWithDummy(any());
    }

    @Test
    void loginWithUnknownEmailFailsAfterComparingWithDummyHash() {
        when(repository.findByEmail("nobody@example.com")).thenReturn(Mono.empty());
        when(passwordHasher.compareWithDummy("password123")).thenReturn(Mono.empty());

        StepVerifier.create(service.login("nobody@example.com", "password123"))
                .verifyError(InvalidCredentialsException.class);
        verify(passwordHasher).compareWithDummy("password123");
    }

    private static User user() {
        return new User(UUID.randomUUID(), "john@example.com", "hash", Instant.now());
    }
}
