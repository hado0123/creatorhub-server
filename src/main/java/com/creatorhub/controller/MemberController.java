package com.creatorhub.controller;

import com.creatorhub.dto.auth.TokenPayload;
import com.creatorhub.dto.member.MemberRequest;
import com.creatorhub.dto.member.MemberResponse;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody MemberRequest req) {
        MemberResponse memberResponse = memberService.signup(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(memberResponse);
    }

    /**
     * 현재 로그인된 사용자 정보 조회 (새로고침 시 인증 상태 복원용)
     */
    @GetMapping("/me")
    public ResponseEntity<TokenPayload> me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        TokenPayload payload = memberService.getTokenPayload(principal.id());
        return ResponseEntity.ok(payload);
    }
}

