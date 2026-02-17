package com.creatorhub.controller;

import com.creatorhub.dto.auth.LoginRequest;
import com.creatorhub.dto.auth.LoginResponse;
import com.creatorhub.dto.auth.RefreshRequest;
import com.creatorhub.dto.auth.TokenPair;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {  // 변경
        LoginResponse loginResponse = authService.login(req.email(), req.password());  // 변경
        return ResponseEntity.ok(loginResponse);  // 변경
    }


    /**
     * refresh 토큰 재발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@Valid @RequestBody RefreshRequest req) {
        TokenPair tokenPair = authService.refresh(req.refreshToken());
        return ResponseEntity.ok(tokenPair);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserPrincipal principal) {
        Long id = principal.id();
        authService.logout(id);
        return ResponseEntity.noContent().build();
   }
}
