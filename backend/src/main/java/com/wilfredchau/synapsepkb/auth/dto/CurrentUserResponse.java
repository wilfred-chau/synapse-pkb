package com.wilfredchau.synapsepkb.auth.dto;

public record CurrentUserResponse(
        Long id,
        String username,
        String displayName,
        String spaceKey) {
}
