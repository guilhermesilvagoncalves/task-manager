package com.taskmanager.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TokenIssuerTest {

    private static final String ISSUER = "http://auth-service:8082";

    private final RSAKey signingKey = new SigningKeyConfig().signingKey();
    private final TokenIssuer issuer = new TokenIssuer(signingKey, ISSUER);
    private final ReactiveJwtDecoder validator = validatorUsing(new JwksController(signingKey).jwks());

    @Test
    void tokenHasTheAgreedClaimsAndLifetime() throws ParseException {
        UUID userId = UUID.randomUUID();

        AccessToken token = issuer.issue(userId);

        assertThat(token.expiresIn()).isEqualTo(3600);
        JWTClaimsSet claims = SignedJWT.parse(token.value()).getJWTClaimsSet();
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant()))
                .isEqualTo(Duration.ofHours(1));
        assertThat(claims.getClaims().keySet()).containsExactlyInAnyOrder("iss", "sub", "iat", "exp");
    }

    @Test
    void tokenKidMatchesAKeyInTheJwks() throws ParseException {
        JWSHeader header = SignedJWT.parse(issuer.issue(UUID.randomUUID()).value()).getHeader();

        assertThat(header.getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
        assertThat(jwks().getKeyByKeyId(header.getKeyID())).isNotNull();
    }

    @Test
    void validatorUsingTheJwksAcceptsIssuedToken() {
        UUID userId = UUID.randomUUID();

        StepVerifier.create(validator.decode(issuer.issue(userId).value()))
                .assertNext(jwt -> assertThat(jwt.getSubject()).isEqualTo(userId.toString()))
                .verifyComplete();
    }

    @Test
    void validatorRejectsTokenSignedByAnotherKeyWithTheSameKid() {
        RSAKey otherKey = new SigningKeyConfig().signingKey();
        RSAKey impostor = new RSAKey.Builder(otherKey).keyID(signingKey.getKeyID()).build();
        String token = new TokenIssuer(impostor, ISSUER).issue(UUID.randomUUID()).value();

        StepVerifier.create(validator.decode(token)).expectError().verify();
    }

    @Test
    void validatorRejectsTamperedPayload() {
        String[] parts = issuer.issue(UUID.randomUUID()).value().split("\\.");
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"iss\":\"" + ISSUER + "\",\"sub\":\"" + UUID.randomUUID() + "\",\"exp\":4102444800}")
                        .getBytes(StandardCharsets.UTF_8));
        String token = parts[0] + "." + tamperedPayload + "." + parts[2];

        StepVerifier.create(validator.decode(token)).expectError().verify();
    }

    @Test
    void validatorRejectsOtherAlgorithm() throws Exception {
        SignedJWT hs256 = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(signingKey.getKeyID()).build(),
                new JWTClaimsSet.Builder().issuer(ISSUER).subject(UUID.randomUUID().toString())
                        .expirationTime(new Date(System.currentTimeMillis() + 60_000)).build());
        hs256.sign(new MACSigner("a-shared-secret-that-is-at-least-256-bits-long!"));

        StepVerifier.create(validator.decode(hs256.serialize())).expectError().verify();
    }

    private JWKSet jwks() {
        try {
            return JWKSet.parse(new JwksController(signingKey).jwks());
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    private ReactiveJwtDecoder validatorUsing(java.util.Map<String, Object> jwksJson) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withJwkSource(jwt -> {
                    try {
                        return Flux.fromIterable(JWKSet.parse(jwksJson).getKeys());
                    } catch (ParseException e) {
                        return Flux.error(e);
                    }
                })
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }
}
