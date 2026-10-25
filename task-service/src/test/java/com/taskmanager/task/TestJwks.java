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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Local JWKS endpoint plus a signer, so tests can build tokens the way the auth service does (spec 002). */
class TestJwks {

    static final String ISSUER = "http://auth-service:8082";

    final RSAKey signingKey = newKey();
    final AtomicInteger requests = new AtomicInteger();
    private final HttpServer server;

    TestJwks() {
        byte[] jwks = new JWKSet(signingKey).toPublicJWKSet().toString().getBytes(StandardCharsets.UTF_8);
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/.well-known/jwks.json", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jwks.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(jwks);
            }
        });
        server.start();
    }

    String uri() {
        return "http://localhost:" + server.getAddress().getPort() + "/.well-known/jwks.json";
    }

    void stop() {
        server.stop(0);
    }

    /** Claims of a token issued by the auth service, valid for one hour. */
    static JWTClaimsSet.Builder validClaims() {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofHours(1))));
    }

    String sign(JWTClaimsSet claims) {
        return sign(signingKey, claims);
    }

    static String sign(RSAKey key, JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
            jwt.sign(new RSASSASigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    static RSAKey newKey() {
        try {
            return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
