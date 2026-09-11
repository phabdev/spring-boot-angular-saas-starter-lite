package com.phabdev.starter;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.phabdev.starter.auth.TokenSecrets;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import javax.crypto.spec.SecretKeySpec;

class AuthenticationIntegrationTest extends IntegrationTestSupport {
    @Test
    void registrationUsesSafeCookieAndServerRole() throws Exception {
        var account = register();
        assertThat(account.refresh().isHttpOnly()).isTrue();
        assertThat(account.refresh().getPath()).isEqualTo("/api/auth");
        assertThat(account.refresh().getAttribute("SameSite")).isEqualTo("Strict");
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + account.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        String hash =
                jdbc.sql("SELECT password_hash FROM app_user WHERE id=:id")
                        .param("id", account.id())
                        .query(String.class)
                        .single();
        assertThat(hash).startsWith("$2").isNotEqualTo(account.password());
        assertThat(
                        jdbc.sql("SELECT token_hash FROM refresh_token WHERE user_id=:id")
                                .param("id", account.id())
                                .query(String.class)
                                .single())
                .isEqualTo(TokenSecrets.hash(account.refresh().getValue()));
        mvc.perform(
                        body(
                                authPost("/api/auth/register"),
                                Map.of(
                                        "email",
                                        "role-" + UUID.randomUUID() + "@example.com",
                                        "password",
                                        account.password(),
                                        "displayName",
                                        "Attacker",
                                        "role",
                                        "ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void loginNormalizesEmailAndRejectsWrongPassword() throws Exception {
        var account = register();
        mvc.perform(
                        body(
                                authPost("/api/auth/login"),
                                Map.of(
                                        "email",
                                        account.email().toUpperCase(),
                                        "password",
                                        account.password())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(900));
        mvc.perform(
                        body(
                                authPost("/api/auth/login"),
                                Map.of("email", account.email(), "password", "wrong-password")))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        body(
                                authPost("/api/auth/register"),
                                Map.of(
                                        "email",
                                        account.email(),
                                        "password",
                                        account.password(),
                                        "displayName",
                                        "Duplicate")))
                .andExpect(status().isConflict());
    }

    @Test
    void refreshRotationRejectsReplayAndRevokesFamily() throws Exception {
        var account = register();
        var rotated =
                mvc.perform(authPost("/api/auth/refresh").cookie(account.refresh()))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getCookie("refresh_token");
        assertThat(rotated.getValue()).isNotEqualTo(account.refresh().getValue());
        mvc.perform(authPost("/api/auth/refresh").cookie(account.refresh()))
                .andExpect(status().isUnauthorized());
        mvc.perform(authPost("/api/auth/refresh").cookie(rotated))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentRefreshCannotCreateTwoUsableSessions() throws Exception {
        var account = register();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first =
                    executor.submit(
                            () ->
                                    mvc.perform(
                                                    authPost("/api/auth/refresh")
                                                            .cookie(account.refresh()))
                                            .andReturn());
            var second =
                    executor.submit(
                            () ->
                                    mvc.perform(
                                                    authPost("/api/auth/refresh")
                                                            .cookie(account.refresh()))
                                            .andReturn());
            var one = first.get();
            var two = second.get();
            assertThat(
                            java.util.List.of(
                                    one.getResponse().getStatus(), two.getResponse().getStatus()))
                    .containsExactlyInAnyOrder(200, 401);
            var winner = one.getResponse().getStatus() == 200 ? one : two;
            mvc.perform(
                            authPost("/api/auth/refresh")
                                    .cookie(winner.getResponse().getCookie("refresh_token")))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void logoutClearsCookieAndRevokesRefresh() throws Exception {
        var account = register();
        var response =
                mvc.perform(authPost("/api/auth/logout").cookie(account.refresh()))
                        .andExpect(status().isNoContent())
                        .andReturn()
                        .getResponse();
        assertThat(response.getCookie("refresh_token").getMaxAge()).isZero();
        mvc.perform(authPost("/api/auth/refresh").cookie(account.refresh()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentLogoutAndRefreshLeaveNoUsableRefreshToken() throws Exception {
        var account = register();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var refresh =
                    executor.submit(
                            () ->
                                    mvc.perform(
                                                    authPost("/api/auth/refresh")
                                                            .cookie(account.refresh()))
                                            .andReturn());
            var logout =
                    executor.submit(
                            () ->
                                    mvc.perform(
                                                    authPost("/api/auth/logout")
                                                            .cookie(account.refresh()))
                                            .andReturn());
            var result = refresh.get();
            assertThat(logout.get().getResponse().getStatus()).isEqualTo(204);
            assertThat(result.getResponse().getStatus()).isIn(200, 401);
            if (result.getResponse().getStatus() == 200)
                mvc.perform(
                                authPost("/api/auth/refresh")
                                        .cookie(result.getResponse().getCookie("refresh_token")))
                        .andExpect(status().isUnauthorized());
            assertThat(
                            jdbc.sql(
                                            "SELECT count(*) FROM refresh_token WHERE user_id=:id"
                                                + " AND revoked=FALSE")
                                    .param("id", account.id())
                                    .query(Integer.class)
                                    .single())
                    .isZero();
        }
    }

    @Test
    void expiredRefreshCannotBeUsed() throws Exception {
        var account = register();
        jdbc.sql(
                        "UPDATE refresh_token SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 second'"
                            + " WHERE user_id=:id")
                .param("id", account.id())
                .update();
        mvc.perform(authPost("/api/auth/refresh").cookie(account.refresh()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestsRequireAuthenticationAndSameOriginAuthHeader() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/auth/refresh")
                                .header("X-Requested-With", "XMLHttpRequest")
                                .header("Origin", "https://attacker.example"))
                .andExpect(status().isForbidden());
        mvc.perform(authPost("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void signedButExpiredJwtAndTamperedJwtAreRejected() throws Exception {
        var account = register();
        var key =
                new SecretKeySpec(
                        JWT_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var claims =
                JwtClaimsSet.builder()
                        .issuer("phabdev-saas-starter")
                        .subject(account.id().toString())
                        .issuedAt(Instant.now().minusSeconds(100))
                        .expiresAt(Instant.now().minusSeconds(1))
                        .claim("ver", 0)
                        .build();
        String expired =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue();
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/api/me")
                                .header(
                                        "Authorization",
                                        "Bearer " + account.accessToken() + "tampered"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordAndDtoValidationRejectsUnsafeInput() throws Exception {
        for (String password : java.util.List.of("short", "界".repeat(30))) {
            mvc.perform(
                            body(
                                    authPost("/api/auth/register"),
                                    Map.of(
                                            "email",
                                            "validation@example.com",
                                            "password",
                                            password,
                                            "displayName",
                                            "Test")))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        body(
                                authPost("/api/auth/register"),
                                Map.of(
                                        "email",
                                        "invalid",
                                        "password",
                                        "valid-test-password",
                                        "displayName",
                                        " ")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        authPost("/api/auth/register")
                                .contentType("application/json")
                                .content("{broken"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthAndOpenApiAreAvailableWithoutCredentials() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
