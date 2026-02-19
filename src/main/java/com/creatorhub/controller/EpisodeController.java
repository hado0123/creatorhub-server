package com.creatorhub.controller;

import com.creatorhub.dto.episode.EpisodeListResponse;
import com.creatorhub.dto.episode.EpisodeRequest;
import com.creatorhub.dto.episode.EpisodeResponse;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.EpisodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/episodes")
@RequiredArgsConstructor
public class EpisodeController {

    private final EpisodeService episodeService;

    /**
     * 회차 등록 + 원고/썸네일 매핑 insert
     */
    @PreAuthorize("hasRole('ROLE_CREATOR')")
    @PostMapping("/create")
    public ResponseEntity<EpisodeResponse> publishEpisode(
            @Valid @RequestBody EpisodeRequest req,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        EpisodeResponse res = episodeService.publishEpisode(req, principal.id());
        return ResponseEntity.ok(res);
    }

    /**
     * 특정 작품의 모든 회차 목록 조회
     */
    @GetMapping("/creation/{creationId}")
    public ResponseEntity<List<EpisodeListResponse>> getEpisodesByCreation(
            @PathVariable Long creationId
    ) {
        List<EpisodeListResponse> res = episodeService.getEpisodesByCreation(creationId);
        return ResponseEntity.ok(res);
    }
}
