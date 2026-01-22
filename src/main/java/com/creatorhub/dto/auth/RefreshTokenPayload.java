package com.creatorhub.dto.auth;

import java.util.Map;

public record RefreshTokenPayload(
        Long id
) {
    public static RefreshTokenPayload from(TokenPayload tokenPayload) {
        return new RefreshTokenPayload(tokenPayload.id());
    }

    /**
     * Refresh 토큰 생성에 필요한 id를 Map으로 변환
     */
    public Map<String, Object> toClaims() {
        return Map.of("id", id);
    }
}
