package com.phabdev.starter.auth;

import com.phabdev.starter.user.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
class DevSeed implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String email;
    private final String password;

    DevSeed(
            UserRepository users,
            PasswordEncoder passwords,
            @Value("${app.seed.admin-email}") String email,
            @Value("${app.seed.admin-password}") String password) {
        this.users = users;
        this.passwords = passwords;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        TokenSecrets.validatePassword(password);
        if (email.isBlank()
                || email.length() > 254
                || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Explicit valid APP_SEED_ADMIN_EMAIL required.");
        String normalized = AuthService.normalizeEmail(email);
        if (users.byEmail(normalized).isEmpty())
            users.create(normalized, passwords.encode(password), "Development admin", Role.ADMIN);
    }
}

@Component
class ExpiredTokenCleanup {
    private final JdbcClient jdbc;

    ExpiredTokenCleanup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    void cleanup() {
        jdbc.sql(
                        "DELETE FROM refresh_token WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL"
                            + " '7 days'")
                .update();
    }
}
