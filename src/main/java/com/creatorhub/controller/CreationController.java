package com.creatorhub.controller;


import com.creatorhub.dto.creation.CreationListResponse;
import com.creatorhub.dto.creation.CreationRequest;
import com.creatorhub.dto.creation.CreationResponse;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.CreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creations")
@RequiredArgsConstructor
public class CreationController {
    private final CreationService creationService;

    /**
     * 작품등록
     */
    @PreAuthorize("hasRole('ROLE_CREATOR')")
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createCreation(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreationRequest req
    ) {
        Long id = creationService.createCreation(req, principal.id());
        return ResponseEntity.ok(Map.of("creationId", id));
    }


    /**
     * 모든 요일별 연재 작품 조회
     */
    @GetMapping("/by-days")
    public ResponseEntity<com.creatorhub.dto.creation.CreationsByDayResponse> getAllCreationsByDay() {
        return ResponseEntity.ok(creationService.getAllCreationsByDay());
    }

    /**
     * 내 작품 목록 조회
     */
    @PreAuthorize("hasRole('ROLE_CREATOR')")
    @GetMapping("/my")
    public ResponseEntity<List<CreationListResponse>> getMyCreations(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(creationService.getMyCreations(principal.id()));
    }

    /**
     * 특정 작품 조회
     */
    @GetMapping("/{creationId}")
    public CreationResponse getCreation(@PathVariable Long creationId) {
        return creationService.getCreation(creationId);
    }
}
