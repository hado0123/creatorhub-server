package com.creatorhub.dto.auth;

public record LoginResponse(
        String accessToken,
        TokenPayload member
) {
    public static LoginResponse of(String accessToken, TokenPayload payload) {
        return new LoginResponse(accessToken, payload);
    }
}