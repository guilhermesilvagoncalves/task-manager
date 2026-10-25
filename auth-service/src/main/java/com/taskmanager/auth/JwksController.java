package com.taskmanager.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Internal: other services read it inside the docker network; the gateway does not route it.
@Hidden
@RestController
public class JwksController {

    private final Map<String, Object> jwks;

    public JwksController(RSAKey signingKey) {
        // toPublicJWKSet() drops every private key parameter.
        this.jwks = new JWKSet(signingKey).toPublicJWKSet().toJSONObject();
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwks;
    }
}
