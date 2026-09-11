package com.phabdev.starter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.phabdev.starter.auth.TokenSecrets;

import jakarta.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

/** Tests always use PostgreSQL. An unavailable Docker daemon is a failure, never a skip. */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {
    static final String JWT_SECRET = TokenSecrets.random();
    static final PostgreSQLContainer POSTGRES;

    static {
        if (System.getenv("TEST_DB_URL") == null || System.getenv("TEST_DB_URL").isBlank()) {
            POSTGRES = new PostgreSQLContainer("postgres:17.11-alpine");
            POSTGRES.start();
        } else {
            POSTGRES = null;
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add(
                "spring.datasource.url",
                () -> POSTGRES != null ? POSTGRES.getJdbcUrl() : System.getenv("TEST_DB_URL"));
        properties.add(
                "spring.datasource.username",
                () ->
                        POSTGRES != null
                                ? POSTGRES.getUsername()
                                : System.getenv().getOrDefault("TEST_DB_USERNAME", "starter"));
        properties.add(
                "spring.datasource.password",
                () ->
                        POSTGRES != null
                                ? POSTGRES.getPassword()
                                : System.getenv("TEST_DB_PASSWORD"));
        properties.add("app.jwt-secret", () -> JWT_SECRET);
        properties.add("app.origin", () -> "http://localhost:4200");
        properties.add("app.cookie-secure", () -> false);
        properties.add("app.auth-rate-limit", () -> 1000);
        properties.add("app.seed.enabled", () -> false);
        properties.add("management.health.mail.enabled", () -> false);
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JdbcClient jdbc;

    protected MockHttpServletRequestBuilder authPost(String path) {
        return post(path)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Origin", "http://localhost:4200");
    }

    protected MockHttpServletRequestBuilder body(
            MockHttpServletRequestBuilder request, Object value) {
        return request.contentType("application/json").content(json.writeValueAsString(value));
    }

    protected Account register() throws Exception {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        String password = "Test-only-" + TokenSecrets.random().substring(0, 16);
        MvcResult result =
                mvc.perform(
                                body(
                                        authPost("/api/auth/register"),
                                        Map.of(
                                                "email",
                                                email,
                                                "password",
                                                password,
                                                "displayName",
                                                "Test User")))
                        .andExpect(status().isCreated())
                        .andReturn();
        var response = json.readTree(result.getResponse().getContentAsString());
        return new Account(
                UUID.fromString(response.get("user").get("id").asString()),
                email,
                password,
                response.get("accessToken").asString(),
                result.getResponse().getCookie("refresh_token"));
    }

    protected record Account(
            UUID id, String email, String password, String accessToken, Cookie refresh) {}
}
