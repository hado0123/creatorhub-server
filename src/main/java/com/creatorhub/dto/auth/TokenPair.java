package com.creatorhub.dto.auth;

public record TokenPair(
        String accessToken,
        String refreshToken
) {}