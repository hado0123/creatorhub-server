package com.creatorhub.controller;

import com.creatorhub.dto.MemberRequest;
import com.creatorhub.dto.MemberResponse;
import com.creatorhub.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
