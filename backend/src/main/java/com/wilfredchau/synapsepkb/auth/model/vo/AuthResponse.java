package com.wilfredchau.synapsepkb.auth.model.vo;

public record AuthResponse(
        String accessToken,
        CurrentUserResponse user) {
}
