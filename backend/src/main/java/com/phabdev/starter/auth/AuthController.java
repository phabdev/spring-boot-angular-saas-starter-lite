package com.phabdev.starter.auth;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@io.swagger.v3.oas.annotations.security.SecurityRequirements
public class AuthController {
    private final AuthService auth;
    private final boolean secure;

    public AuthController(AuthService auth, @Value("${app.cookie-secure}") boolean secure) {
        this.auth = auth;
        this.secure = secure;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.Session> register(@Valid @RequestBody AuthDtos.Register body) {
        return respond(auth.register(body), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.Session> login(@Valid @RequestBody AuthDtos.Login body) {
        return respond(auth.login(body), HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.Session> refresh(
            @CookieValue(name = "refresh_token", required = false) String token) {
        return respond(auth.refresh(token), HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String token) {
        auth.logout(token);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    private ResponseEntity<AuthDtos.Session> respond(
            AuthDtos.IssuedSession session, HttpStatus status) {
        return ResponseEntity.status(status)
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie(session.refreshToken(), Duration.ofDays(auth.refreshDays())))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(session.session());
    }

    private String cookie(String value, Duration age) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(age)
                .build()
                .toString();
    }
}
