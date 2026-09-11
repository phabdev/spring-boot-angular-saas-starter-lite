package com.phabdev.starter.auth;

import com.phabdev.starter.user.UserView;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record Register(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password,
            @NotBlank @Size(max = 100) String displayName) {}

    public record Login(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {}

    public record Session(String accessToken, String tokenType, long expiresIn, UserView user) {}

    public record IssuedSession(Session session, String refreshToken) {}
}
