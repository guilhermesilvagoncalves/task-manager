package com.taskmanager.task;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Validates tokens the same way the auth service issues them (spec 002 token contract). */
class JwtDecoderTest {

    private static final String ISSUER = "http://auth-service:8082";

    private final RSAKey signingKey = newKey();
    private final AtomicInteger jwksRequests = new AtomicInteger();
    private HttpServer jwksServer;
    private ReactiveJwtDecoder decoder;

    @BeforeEach
    void startJwksServer() throws IOException {
        byte[] jwks = new JWKSet(signingKey).toPublicJWKSet().toString().getBytes(StandardCharsets.UTF_8);
        jwksServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        jwksServer.createContext("/.well-known/jwks.json", exchange -> {
            jwksRequests.incrementAndGet();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(jwks);
            }
        });
        jwksServer.start();
        String jwksUri = "http://localhost:" + jwksServer.getAddress().getPort() + "/.well-known/jwks.json";
        decoder = new SecurityConfig().jwtDecoder(jwksUri, ISSUER);
    }

    @AfterEach
    void stopJwksServer() {
        jwksServer.stop(0);
    }

    @Test
    void acceptsValidToken() {
        String subject = UUID.randomUUID().toString();

        StepVerifier.create(decoder.decode(token(signingKey, ISSUER, subject, Instant.now().plus(Duration.ofHours(1)))))
                .assertNext(jwt -> assertThat(jwt.getSubject()).isEqualTo(subject))
                .verifyComplete();
    }

    @Test
    void fetchesTheJwksOnceForManyRequests() {
        for (int i = 0; i < 3; i++) {
            StepVerifier.create(decoder.decode(validToken())).expectNextCount(1).verifyComplete();
        }

        assertThat(jwksRequests).hasValue(1);
    }

    @Test
    void rejectsExpiredToken() {
        String expired = token(signingKey, ISSUER, UUID.randomUUID().toString(), Instant.now().minus(Duration.ofMinutes(5)));

        StepVerifier.create(decoder.decode(expired)).expectError().verify();
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        String other = token(signingKey, "http://evil", UUID.randomUUID().toString(), Instant.now().plus(Duration.ofHours(1)));

        StepVerifier.create(decoder.decode(other)).expectError().verify();
    }

    @Test
    void rejectsTokenSignedByAnotherKeyWithTheSameKid() {
        RSAKey impostor = new RSAKey.Builder(newKey()).keyID(signingKey.getKeyID()).build();
        String forged = token(impostor, ISSUER, UUID.randomUUID().toString(), Instant.now().plus(Duration.ofHours(1)));

        StepVerifier.create(decoder.decode(forged)).expectError().verify();
    }

    @Test
    void rejectsMalformedToken() {
        StepVerifier.create(decoder.decode("not-a-jwt")).expectError().verify();
    }

    private String validToken() {
        return token(signingKey, ISSUER, UUID.randomUUID().toString(), Instant.now().plus(Duration.ofHours(1)));
    }

    private static String token(RSAKey key, String issuer, String subject, Instant expiresAt) {
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                    new JWTClaimsSet.Builder()
                            .issuer(issuer)
                            .subject(subject)
                            .issueTime(Date.from(expiresAt.minus(Duration.ofHours(1))))
                            .expirationTime(Date.from(expiresAt))
                            .build());
            jwt.sign(new RSASSASigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RSAKey newKey() {
        try {
            return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
