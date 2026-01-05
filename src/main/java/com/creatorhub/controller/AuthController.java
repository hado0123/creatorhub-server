package com.creatorhub.controller;

import com.creatorhub.dto.*;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@RequestBody LoginRequest req) {
        log.info("로그인 요청 - email={}", req.email());
        TokenPair tokenPair = authService.login(req.email(), req.password());
        return ResponseEntity.ok(tokenPair);
    }

    /**
     * refresh 토큰 재발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestBody RefreshRequest req) {
        log.info("토큰 재발급 요청");
        TokenPair tokenPair = authService.refresh(req.refreshToken());
        return ResponseEntity.ok(tokenPair);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserPrincipal principal) {
        Long id = principal.id();
        log.info("로그아웃 요청 - id={}", id);
        authService.logout(id);
        return ResponseEntity.noContent().build();
   }
}
