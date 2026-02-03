package com.creatorhub.controller;

import com.creatorhub.dto.creator.CreatorRequest;
import com.creatorhub.dto.creator.CreatorResponse;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.CreatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorService creatorService;
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
