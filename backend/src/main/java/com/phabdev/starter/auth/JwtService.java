package com.phabdev.starter.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.phabdev.starter.user.UserAccount;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.spec.SecretKeySpec;

@Service
public class JwtService {
    private static final String ISSUER = "phabdev-saas-starter";
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long lifetime;

    public JwtService(
            @Value("${app.jwt-secret}") String secret,
            @Value("${app.access-seconds}") long lifetime) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32 || lifetime < 30 || lifetime > 3600)
            throw new IllegalArgumentException(
                    "JWT_SECRET requires 32+ bytes; access lifetime must be 30..3600 seconds.");
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var configured =
                NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        configured.setJwtValidator(
                new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                        new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator(ISSUER)));
        decoder = configured;
        this.lifetime = lifetime;
    }

    public String issue(UserAccount user) {
        Instant now = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer(ISSUER)
                        .subject(user.id().toString())
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(lifetime))
                        .claim("ver", user.authVersion())
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    public long lifetime() {
        return lifetime;
    }
}
