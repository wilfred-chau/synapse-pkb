package com.wilfredchau.synapsepkb.auth.model.vo;

public record CurrentUserResponse(
        Long id,
        String username,
        String displayName,
        String spaceKey) {
}
