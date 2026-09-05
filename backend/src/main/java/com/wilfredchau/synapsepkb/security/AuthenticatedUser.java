package com.wilfredchau.synapsepkb.security;

public record AuthenticatedUser(
        Long id,
        String username,
        String displayName,
        String spaceKey) {
}
