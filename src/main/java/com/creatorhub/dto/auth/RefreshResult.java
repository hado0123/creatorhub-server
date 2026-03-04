package com.creatorhub.dto.auth;

public record RefreshResult(
        String accessToken,
        String refreshToken,
        boolean keepLogin
) {
    public static RefreshResult of(String accessToken, String refreshToken, Boolean keepLogin) {
        return new RefreshResult(accessToken, refreshToken, keepLogin);
    }
}