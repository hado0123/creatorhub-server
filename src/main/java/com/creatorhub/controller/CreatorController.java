package com.creatorhub.controller;

import com.creatorhub.dto.CreatorRequest;
import com.creatorhub.dto.CreatorResponse;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.CreatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
@Slf4j
public class CreatorController {

    private final CreatorService creatorService;

    // 인가 테스트용(삭제 예정)
    @PreAuthorize("hasRole('ROLE_CREATOR')")
    @GetMapping("/list")
    public ResponseEntity<?> list() {
        log.info("creator list............");
        String[] arr = {"AAA", "BBB", "CCC"};
        return ResponseEntity.ok(arr);
    }

    /**
     * 작가등록
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("/signup")
    public ResponseEntity<CreatorResponse> signup(
            @Valid @RequestBody CreatorRequest req,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        CreatorResponse creatorResponse = creatorService.signup(principal.id(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(creatorResponse);
    }
}
