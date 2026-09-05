package com.wilfredchau.synapsepkb.auth.dto;

public record AuthResponse(
        String accessToken,
        CurrentUserResponse user) {
}
