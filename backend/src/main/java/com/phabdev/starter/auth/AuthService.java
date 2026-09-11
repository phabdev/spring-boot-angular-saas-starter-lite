package com.phabdev.starter.auth;

import com.phabdev.starter.common.ApiException;
import com.phabdev.starter.user.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final JdbcClient jdbc;
    private final int refreshDays;
    private final String dummyHash;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwords,
            JwtService jwt,
            JdbcClient jdbc,
            @Value("${app.refresh-days}") int refreshDays) {
        if (refreshDays < 1 || refreshDays > 90)
            throw new IllegalArgumentException("Refresh days must be 1..90.");
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
        this.jdbc = jdbc;
        this.refreshDays = refreshDays;
        dummyHash = passwords.encode(TokenSecrets.random());
    }

    @Transactional
    public AuthDtos.IssuedSession register(AuthDtos.Register request) {
        TokenSecrets.validatePassword(request.password());
        String email = normalizeEmail(request.email());
        if (users.byEmail(email).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "Unable to register this account.");
        UserAccount user =
                users.create(
                        email,
                        passwords.encode(request.password()),
                        request.displayName().trim(),
                        Role.USER);
        return issue(user, UUID.randomUUID());
    }

    @Transactional
    public AuthDtos.IssuedSession login(AuthDtos.Login request) {
        jdbc.sql("SELECT id FROM app_user WHERE email=:email FOR UPDATE")
                .param("email", normalizeEmail(request.email()))
                .query(UUID.class)
                .optional();
        var account = users.byEmail(normalizeEmail(request.email()));
        boolean matches =
                request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72
                        && passwords.matches(
                                request.password(),
                                account.map(UserAccount::passwordHash).orElse(dummyHash));
        if (account.isEmpty() || !matches) throw ApiException.unauthorized();
        return issue(account.orElseThrow(), UUID.randomUUID());
    }

    // Replay revocation must commit even though the request returns 401.
    @Transactional(noRollbackFor = ApiException.class)
    public AuthDtos.IssuedSession refresh(String raw) {
        if (raw == null || raw.length() > 100) throw ApiException.unauthorized();
        UUID owner =
                jdbc.sql("SELECT user_id FROM refresh_token WHERE token_hash=:hash")
                        .param("hash", TokenSecrets.hash(raw))
                        .query(UUID.class)
                        .optional()
                        .orElseThrow(ApiException::unauthorized);
        jdbc.sql("SELECT id FROM app_user WHERE id=:id FOR UPDATE")
                .param("id", owner)
                .query(UUID.class)
                .optional()
                .orElseThrow(ApiException::unauthorized);
        var token =
                jdbc.sql(
                                "SELECT user_id,family_id,expires_at,revoked FROM refresh_token"
                                    + " WHERE token_hash=:hash FOR UPDATE")
                        .param("hash", TokenSecrets.hash(raw))
                        .query(
                                (rs, row) ->
                                        new Refresh(
                                                rs.getObject("user_id", UUID.class),
                                                rs.getObject("family_id", UUID.class),
                                                rs.getTimestamp("expires_at").toInstant(),
                                                rs.getBoolean("revoked")))
                        .optional()
                        .orElseThrow(ApiException::unauthorized);
        if (token.revoked() || !token.expiresAt().isAfter(Instant.now())) {
            jdbc.sql("UPDATE refresh_token SET revoked=TRUE WHERE family_id=:id")
                    .param("id", token.familyId())
                    .update();
            throw ApiException.unauthorized();
        }
        jdbc.sql("UPDATE refresh_token SET revoked=TRUE WHERE token_hash=:hash")
                .param("hash", TokenSecrets.hash(raw))
                .update();
        return issue(
                users.byId(token.userId()).orElseThrow(ApiException::unauthorized),
                token.familyId());
    }

    @Transactional
    public void logout(String raw) {
        if (raw == null || raw.length() > 100) return;
        var owner =
                jdbc.sql("SELECT user_id FROM refresh_token WHERE token_hash=:hash")
                        .param("hash", TokenSecrets.hash(raw))
                        .query(UUID.class)
                        .optional();
        if (owner.isEmpty()) return;
        // Use the same account lock as refresh, login and password reset.
        jdbc.sql("SELECT id FROM app_user WHERE id=:id FOR UPDATE")
                .param("id", owner.get())
                .query(UUID.class)
                .optional();
        jdbc.sql(
                        "UPDATE refresh_token SET revoked=TRUE WHERE family_id IN (SELECT family_id"
                            + " FROM refresh_token WHERE token_hash=:hash)")
                .param("hash", TokenSecrets.hash(raw))
                .update();
    }

    private AuthDtos.IssuedSession issue(UserAccount user, UUID family) {
        String raw = TokenSecrets.random();
        jdbc.sql(
                        "INSERT INTO refresh_token(token_hash,user_id,family_id,expires_at)"
                            + " VALUES(:hash,:user,:family,:expiry)")
                .param("hash", TokenSecrets.hash(raw))
                .param("user", user.id())
                .param("family", family)
                .param(
                        "expiry",
                        java.sql.Timestamp.from(Instant.now().plus(refreshDays, ChronoUnit.DAYS)))
                .update();
        return new AuthDtos.IssuedSession(
                new AuthDtos.Session(jwt.issue(user), "Bearer", jwt.lifetime(), user.view()), raw);
    }

    public int refreshDays() {
        return refreshDays;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record Refresh(UUID userId, UUID familyId, Instant expiresAt, boolean revoked) {}
}
