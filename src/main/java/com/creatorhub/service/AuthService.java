package com.creatorhub.service;

import com.creatorhub.constant.ErrorCode;
import com.creatorhub.dto.auth.RefreshTokenPayload;
import com.creatorhub.dto.auth.TokenPair;
import com.creatorhub.dto.auth.TokenPayload;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.MemberNotFoundException;
import com.creatorhub.repository.MemberRepository;
import com.creatorhub.security.exception.JwtAuthenticationException;
import com.creatorhub.security.utils.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;


    /**
     * 로그인: 인증 + 토큰 발급
     */
    public TokenPair login(String email, String rawPassword) {

        // 1) 이메일/비번 검증 (실제 로그인)
        TokenPayload payload = memberService.authenticate(email, rawPassword);

        // 2) 토큰 발급
        String accessToken = jwtUtil.createAccessToken(payload);
        String refreshToken = jwtUtil.createRefreshToken(RefreshTokenPayload.from(payload));

        // 3) Refresh 토큰 Redis 저장
        refreshTokenService.saveRefreshToken(payload.id(), refreshToken);

        log.info("로그인 성공 - memberId={}, role={}", payload.id(), payload.role());

        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Refresh 토큰 기반 재발급
     */
    public TokenPair refresh(String refreshToken) {

        // 1) Refresh 토큰 검증
        RefreshTokenPayload refreshPayload;
        try {
            refreshPayload = jwtUtil.validateRefreshToken(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException(ErrorCode.EXPIRE_REFRESH_TOKEN, e);
        } catch (JwtException e) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN, e);
        }

        Long id = refreshPayload.id();

        // 2) Redis에 저장된 Refresh 토큰과 일치하는지 확인
        String storedRefreshToken = refreshTokenService.getRefreshToken(id);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3) 유저 정보 조회
        Member member = memberRepository.findById(id)
                .orElseThrow(MemberNotFoundException::new);
        TokenPayload newPayload = TokenPayload.from(member);

        // 4) 새 토큰 발급 (refresh 토큰도 새로 갱신)
        String newAccessToken = jwtUtil.createAccessToken(newPayload);
        String newRefreshToken = jwtUtil.createRefreshToken(RefreshTokenPayload.from(newPayload));

        // 5) Redis 갱신 (이전 refresh는 자동 폐기)
        refreshTokenService.saveRefreshToken(id, newRefreshToken);

        log.info("토큰 재발급 성공 - memberId={}", id);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃
     */
    public void logout(Long id) {
        refreshTokenService.deleteRefreshToken(id);
        log.info("로그아웃 성공 - memberId={}", id);
    }
}
