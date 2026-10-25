package com.taskmanager.task.config;

import com.taskmanager.task.TestJwks;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/** Validates tokens the same way the auth service issues them (spec 002 token contract, spec 005 claims). */
class JwtDecoderTest {

    private TestJwks jwks;
    private ReactiveJwtDecoder decoder;

    @BeforeEach
    void startJwksServer() {
        jwks = new TestJwks();
        decoder = new SecurityConfig().jwtDecoder(jwks.uri(), TestJwks.ISSUER);
    }

    @AfterEach
    void stopJwksServer() {
        jwks.stop();
    }

    @Test
    void acceptsValidToken() {
        var claims = TestJwks.validClaims().build();

        StepVerifier.create(decoder.decode(jwks.sign(claims)))
                .assertNext(jwt -> assertThat(jwt.getSubject()).isEqualTo(claims.getSubject()))
                .verifyComplete();
    }

    @Test
    void fetchesTheJwksOnceForManyRequests() {
        for (int i = 0; i < 3; i++) {
            StepVerifier.create(decoder.decode(jwks.sign(TestJwks.validClaims().build()))).expectNextCount(1).verifyComplete();
        }

        assertThat(jwks.requests).hasValue(1);
    }

    @Test
    void rejectsExpiredToken() {
        Instant past = Instant.now().minus(Duration.ofMinutes(5));
        String expired = jwks.sign(TestJwks.validClaims()
                .issueTime(Date.from(past.minus(Duration.ofHours(1))))
                .expirationTime(Date.from(past))
                .build());

        StepVerifier.create(decoder.decode(expired)).expectError().verify();
    }

    @Test
    void rejectsTokenWithoutExpiry() {
        StepVerifier.create(decoder.decode(jwks.sign(TestJwks.validClaims().expirationTime(null).build())))
                .expectError().verify();
    }

    @Test
    void rejectsTokenWithoutSubject() {
        StepVerifier.create(decoder.decode(jwks.sign(TestJwks.validClaims().subject(null).build())))
                .expectError().verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "admin", "00000000-0000-0000-0000"})
    void rejectsTokenWithBlankOrNonUuidSubject(String subject) {
        StepVerifier.create(decoder.decode(jwks.sign(TestJwks.validClaims().subject(subject).build())))
                .expectError().verify();
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        StepVerifier.create(decoder.decode(jwks.sign(TestJwks.validClaims().issuer("http://evil").build())))
                .expectError().verify();
    }

    @Test
    void rejectsTokenSignedByAnotherKeyWithTheSameKid() {
        RSAKey impostor = new RSAKey.Builder(TestJwks.newKey()).keyID(jwks.signingKey.getKeyID()).build();

        StepVerifier.create(decoder.decode(TestJwks.sign(impostor, TestJwks.validClaims().build())))
                .expectError().verify();
    }

    @Test
    void rejectsAlgorithmOtherThanRs256() throws JOSEException {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS512).keyID(jwks.signingKey.getKeyID()).build(),
                TestJwks.validClaims().build());
        jwt.sign(new RSASSASigner(jwks.signingKey));

        StepVerifier.create(decoder.decode(jwt.serialize())).expectError().verify();
    }

    @Test
    void rejectsMalformedToken() {
        StepVerifier.create(decoder.decode("not-a-jwt")).expectError().verify();
    }
}
