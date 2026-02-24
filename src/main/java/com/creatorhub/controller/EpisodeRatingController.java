package com.creatorhub.controller;


import com.creatorhub.dto.episode.rating.EpisodeRatingRequest;
import com.creatorhub.dto.episode.rating.EpisodeRatingStatusResponse;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.EpisodeRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/episodes/ratings")
public class EpisodeRatingController {

    private final EpisodeRatingService episodeRatingService;

    /**
     * 내 평점 조회
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @GetMapping("/{episodeId}/status")
    public ResponseEntity<EpisodeRatingStatusResponse> getRatingStatus(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long episodeId
    ) {
        return ResponseEntity.ok(episodeRatingService.getRatingStatus(principal.id(), episodeId));
    }

    /**
     * 별점 등록
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("/create")
    public ResponseEntity<EpisodeRatingStatusResponse> rate(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody EpisodeRatingRequest req
    ) {
        return ResponseEntity.ok(episodeRatingService.rate(principal.id(), req.episodeId(), req.score()));
    }
}