package com.phabdev.starter.auth;

import com.phabdev.starter.common.ApiException;

import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class TokenSecrets {
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenSecrets() {}

    public static String random() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static void validatePassword(String password) {
        if (password == null
                || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Password must contain at least 12 characters and at most 72 UTF-8 bytes.");
    }
}
