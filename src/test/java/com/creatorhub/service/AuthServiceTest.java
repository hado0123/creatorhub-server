package com.creatorhub.service;

import com.creatorhub.constant.ErrorCode;
import com.creatorhub.constant.Role;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock MemberService memberService;
    @Mock MemberRepository memberRepository;
    @Mock JWTUtil jwtUtil;
    @Mock RefreshTokenService refreshTokenService;

    @InjectMocks AuthService authService;

    @Test
    @DisplayName("로그인 성공: 인증 후 access/refresh 토큰 발급 + Redis에 refresh 저장")
    void loginSuccess() {
        String email = "test@test.com";
        String rawPassword = "pw1234";

        TokenPayload payload = mock(TokenPayload.class);
        given(payload.id()).willReturn(1L);
        given(payload.role()).willReturn(Role.MEMBER);

        given(memberService.authenticate(email, rawPassword)).willReturn(payload);

        given(jwtUtil.createAccessToken(payload)).willReturn("ACCESS");
        given(jwtUtil.createRefreshToken(any(RefreshTokenPayload.class))).willReturn("REFRESH");

        TokenPair result = authService.login(email, rawPassword);

        assertThat(result.accessToken()).isEqualTo("ACCESS");
        assertThat(result.refreshToken()).isEqualTo("REFRESH");

        InOrder inOrder = inOrder(memberService, jwtUtil, refreshTokenService);
        inOrder.verify(memberService).authenticate(email, rawPassword);
        inOrder.verify(jwtUtil).createAccessToken(payload);
        inOrder.verify(jwtUtil).createRefreshToken(any(RefreshTokenPayload.class));
        inOrder.verify(refreshTokenService).saveRefreshToken(1L, "REFRESH");
    }

    @Test
    @DisplayName("토큰 재발급 실패: refresh 토큰 만료면 EXPIRE_REFRESH_TOKEN")
    void refreshExpired() {
        String refreshToken = "REFRESH";
        given(jwtUtil.validateRefreshToken(refreshToken)).willThrow(mock(ExpiredJwtException.class));

        Throwable thrown = catchThrowable(() -> authService.refresh(refreshToken));

        assertThat(thrown).isInstanceOf(JwtAuthenticationException.class);
        assertThat(((JwtAuthenticationException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.EXPIRE_REFRESH_TOKEN);

        then(refreshTokenService).should(never()).getRefreshToken(anyLong());
        then(memberRepository).should(never()).findById(anyLong());
    }

    @Test
    @DisplayName("토큰 재발급 실패: refresh 토큰이 유효하지 않으면 INVALID_REFRESH_TOKEN")
    void refreshInvalidJwt() {
        String refreshToken = "REFRESH";
        given(jwtUtil.validateRefreshToken(refreshToken)).willThrow(new JwtException("invalid"));

        Throwable thrown = catchThrowable(() -> authService.refresh(refreshToken));

        assertThat(thrown).isInstanceOf(JwtAuthenticationException.class);
        assertThat(((JwtAuthenticationException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        then(refreshTokenService).should(never()).getRefreshToken(anyLong());
        then(memberRepository).should(never()).findById(anyLong());
    }

    @Test
    @DisplayName("토큰 재발급 실패: Redis에 저장된 refresh가 없으면 INVALID_REFRESH_TOKEN")
    void refreshStoredNull() {
        String refreshToken = "REFRESH";

        RefreshTokenPayload refreshPayload = mock(RefreshTokenPayload.class);
        given(refreshPayload.id()).willReturn(1L);

        given(jwtUtil.validateRefreshToken(refreshToken)).willReturn(refreshPayload);
        given(refreshTokenService.getRefreshToken(1L)).willReturn(null);

        Throwable thrown = catchThrowable(() -> authService.refresh(refreshToken));

        assertThat(thrown).isInstanceOf(JwtAuthenticationException.class);
        assertThat(((JwtAuthenticationException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        then(memberRepository).should(never()).findById(anyLong());
        then(jwtUtil).should(never()).createAccessToken(any());
        then(jwtUtil).should(never()).createRefreshToken(any());
        then(refreshTokenService).should(never()).saveRefreshToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("토큰 재발급 실패: Redis refresh가 다르면 INVALID_REFRESH_TOKEN")
    void refreshStoredMismatch() {
        String refreshToken = "REFRESH";

        RefreshTokenPayload refreshPayload = mock(RefreshTokenPayload.class);
        given(refreshPayload.id()).willReturn(1L);

        given(jwtUtil.validateRefreshToken(refreshToken)).willReturn(refreshPayload);
        given(refreshTokenService.getRefreshToken(1L)).willReturn("DIFFERENT");

        Throwable thrown = catchThrowable(() -> authService.refresh(refreshToken));

        assertThat(thrown).isInstanceOf(JwtAuthenticationException.class);
        assertThat(((JwtAuthenticationException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

        then(memberRepository).should(never()).findById(anyLong());
        then(jwtUtil).should(never()).createAccessToken(any());
        then(jwtUtil).should(never()).createRefreshToken(any());
        then(refreshTokenService).should(never()).saveRefreshToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("토큰 재발급 실패: 회원이 없으면 MemberNotFoundException")
    void refreshMemberNotFound() {
        String refreshToken = "REFRESH";

        RefreshTokenPayload refreshPayload = mock(RefreshTokenPayload.class);
        given(refreshPayload.id()).willReturn(1L);

        given(jwtUtil.validateRefreshToken(refreshToken)).willReturn(refreshPayload);
        given(refreshTokenService.getRefreshToken(1L)).willReturn(refreshToken);
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(MemberNotFoundException.class);

        then(jwtUtil).should(never()).createAccessToken(any());
        then(jwtUtil).should(never()).createRefreshToken(any());
        then(refreshTokenService).should(never()).saveRefreshToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("토큰 재발급 성공: 검증/일치/회원조회 후 새 토큰 발급 + Redis 갱신")
    void refreshSuccess() {
        String refreshToken = "REFRESH";

        RefreshTokenPayload refreshPayload = mock(RefreshTokenPayload.class);
        given(refreshPayload.id()).willReturn(1L);

        given(jwtUtil.validateRefreshToken(refreshToken)).willReturn(refreshPayload);
        given(refreshTokenService.getRefreshToken(1L)).willReturn(refreshToken);

        Member member = mock(Member.class);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        given(jwtUtil.createAccessToken(any(TokenPayload.class))).willReturn("NEW_ACCESS");
        given(jwtUtil.createRefreshToken(any(RefreshTokenPayload.class))).willReturn("NEW_REFRESH");

        TokenPair result = authService.refresh(refreshToken);

        assertThat(result.accessToken()).isEqualTo("NEW_ACCESS");
        assertThat(result.refreshToken()).isEqualTo("NEW_REFRESH");

        then(refreshTokenService).should(times(1)).saveRefreshToken(1L, "NEW_REFRESH");
    }

    @Test
    @DisplayName("로그아웃 성공: Redis refresh 삭제 호출")
    void logoutSuccess() {
        authService.logout(1L);

        then(refreshTokenService).should(times(1)).deleteRefreshToken(1L);
    }
}
