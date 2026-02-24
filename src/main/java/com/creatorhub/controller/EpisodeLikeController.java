package com.creatorhub.controller;

import com.creatorhub.dto.episode.like.EpisodeLikeResponse;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.EpisodeLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/episodes")
public class EpisodeLikeController {
    private final EpisodeLikeService episodeLikeService;

    /**
     * 특정 에피소드 좋아요
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("/{episodeId}/like")
    public ResponseEntity<EpisodeLikeResponse> like(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long episodeId
    ) {
        EpisodeLikeResponse episodeLikeResponse
                = episodeLikeService.like(principal.id(), episodeId);

        return ResponseEntity.ok(episodeLikeResponse);
    }

    /**
     * 특정 에피소드 좋아요 여부 조회
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @GetMapping("/{episodeId}/like/status")
    public ResponseEntity<EpisodeLikeResponse> getLikeStatus(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long episodeId
    ) {
        return ResponseEntity.ok(episodeLikeService.getLikeStatus(principal.id(), episodeId));
    }

    /**
     * 특정 에피소드 좋아요 해제
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @DeleteMapping("/{episodeId}/like")
    public ResponseEntity<EpisodeLikeResponse> unlike(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long episodeId
    ) {
        EpisodeLikeResponse episodeLikeResponse
                = episodeLikeService.unlike(principal.id(), episodeId);

        return ResponseEntity.ok(episodeLikeResponse);
    }
}
