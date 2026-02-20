package com.creatorhub.controller;

import com.creatorhub.dto.auth.LoginRequest;
import com.creatorhub.dto.auth.LoginResponse;
import com.creatorhub.dto.auth.TokenPair;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${jwt.refresh-token-exp-days}")
    long refreshTokenExpDays;

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final int SESSION_COOKIE = -1; // 브라우저 종료 시 삭제

    /**
     * 로그인 - accessToken은 body, refreshToken은 HttpOnly Cookie
     * keepLogin=true  -> maxAge: refreshTokenExpDays (브라우저 종료 후에도 유지)
     * keepLogin=false -> Session Cookie, maxAge=-1 (브라우저 종료 시 삭제)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        TokenPair tokenPair = authService.login(req.email(), req.password());

        int maxAge = req.keepLogin() ? (int) (refreshTokenExpDays * 24 * 60 * 60) : SESSION_COOKIE;
        ResponseCookie refreshCookie = buildRefreshCookie(tokenPair.refreshToken(), maxAge);
        LoginResponse body = LoginResponse.of(tokenPair.accessToken(), authService.getTokenPayload(req.email()));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }

    /**
     * AccessToken 재발급 - refreshToken은 Cookie에서 읽음, 새 accessToken만 body로 응답
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken == null || refreshToken.isBlank()) {
            // 비로그인 상태: 정상 흐름으로 204 내려주기
            return ResponseEntity.noContent().build();
        }

        TokenPair tokenPair = authService.refresh(refreshToken);
        // refresh 시엔 기존 쿠키 만료일 유지: 남은 기간을 알 수 없으므로 refreshTokenExpDays 그대로 연장
        ResponseCookie refreshCookie = buildRefreshCookie(tokenPair.refreshToken(), (int)(refreshTokenExpDays * 24 * 60 * 60));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new TokenPair(tokenPair.accessToken(), null));
    }

    /**
     * 로그아웃 - Cookie 만료 처리
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserPrincipal principal) {
        Long id = principal.id();
        authService.logout(id);

        ResponseCookie expiredCookie = buildRefreshCookie("", 0);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /** maxAge: 양수=초단위 지속, -1=Session Cookie, 0=즉시 삭제 */
    private ResponseCookie buildRefreshCookie(String value, int maxAge) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
