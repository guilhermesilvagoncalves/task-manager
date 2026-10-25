package com.taskmanager.auth;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final AuthService service = new AuthService(repository, passwordHasher);

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
}
