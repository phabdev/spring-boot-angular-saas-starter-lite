package com.phabdev.starter.user;

import java.util.UUID;

public record UserAccount(
        UUID id,
        String email,
        String passwordHash,
        String displayName,
        Role role,
        int authVersion) {
    public UserView view() {
        return new UserView(id, email, displayName, role);
    }
}
