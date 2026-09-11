package com.phabdev.starter.user;

import java.util.UUID;

public record UserView(UUID id, String email, String displayName, Role role) {}
