package com.creatorhub.dto.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        TokenPayload member
) {
    public static LoginResponse of(TokenPair tokenPair, TokenPayload payload) {
        return new LoginResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                payload
        );
    }
}