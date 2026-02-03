package com.creatorhub.dto.auth;

import com.creatorhub.constant.Role;
import com.creatorhub.entity.Member;
import java.util.HashMap;
import java.util.Map;

public record TokenPayload(
        Long id,
        String name,
        String email,
        Role role
) {
    public static TokenPayload from(Member member) {
        return new TokenPayload(
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getRole()
        );
    }

    /**
     * JWT 토큰 생성에 필요한 claims를 Map으로 변환
     */
    public Map<String, Object> toClaims() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("email", email);
        map.put("role", role.name()); // ENUM → String
        return map;
    }
}
