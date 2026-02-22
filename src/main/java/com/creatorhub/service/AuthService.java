package com.creatorhub.service;

import com.creatorhub.constant.ErrorCode;
import com.creatorhub.dto.auth.RefreshResult;
import com.creatorhub.dto.auth.RefreshTokenPayload;
import com.creatorhub.dto.auth.TokenPair;
import com.creatorhub.dto.auth.TokenPayload;
import com.creatorhub.entity.Member;
import com.creatorhub.exception.member.MemberNotFoundException;
import com.creatorhub.repository.MemberRepository;
import com.creatorhub.exception.auth.JwtAuthenticationException;
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
    public TokenPair login(String email, String rawPassword, boolean keepLogin) {
        TokenPayload payload = memberService.authenticate(email, rawPassword);

        String accessToken = jwtUtil.createAccessToken(payload);
        String refreshToken = jwtUtil.createRefreshToken(
                RefreshTokenPayload.from(payload, keepLogin)
        );

        refreshTokenService.saveRefreshToken(payload.id(), refreshToken);

        log.debug("로그인 성공 - memberId={}, role={}", payload.id(), payload.role());

        return TokenPair.of(accessToken, refreshToken);
    }

    /**
     * email로 TokenPayload 조회 (로그인 후 member 정보 반환용)
     */
    public TokenPayload getTokenPayload(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);
        return TokenPayload.from(member);
    }

    /**
     * Refresh 토큰 기반 재발급 (쿠키에서 전달받은 refreshToken)
     */
    public RefreshResult refresh(String refreshToken) {
        RefreshTokenPayload refreshPayload;
        try {
            refreshPayload = jwtUtil.validateRefreshToken(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException(ErrorCode.EXPIRE_REFRESH_TOKEN, e);
        } catch (JwtException e) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN, e);
        }

        Long id = refreshPayload.id();
        boolean keepLogin = refreshPayload.keepLogin();

        String storedRefreshToken = refreshTokenService.getRefreshToken(id);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findById(id)
                .orElseThrow(MemberNotFoundException::new);
        TokenPayload newPayload = TokenPayload.from(member);

        String newAccessToken = jwtUtil.createAccessToken(newPayload);

        String newRefreshToken = jwtUtil.createRefreshToken(
                RefreshTokenPayload.from(newPayload, keepLogin)
        );
        refreshTokenService.saveRefreshToken(id, newRefreshToken);

        log.debug("토큰 재발급 성공 - memberId={}", id);

        return RefreshResult.of(newAccessToken, newRefreshToken, keepLogin);
    }

    /**
     * 로그아웃
     */
    public void logout(Long id) {
        refreshTokenService.deleteRefreshToken(id);
        log.debug("로그아웃 성공 - memberId={}", id);
    }
}