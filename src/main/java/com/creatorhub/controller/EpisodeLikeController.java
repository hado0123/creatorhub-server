package com.creatorhub.controller;

import com.creatorhub.dto.EpisodeLikeResponse;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.EpisodeLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/episodes")
public class EpisodeLikeController {
    private final EpisodeLikeService episodeLikeService;

    @PostMapping("/{episodeId}/like")
    public ResponseEntity<EpisodeLikeResponse> like(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long episodeId
    ) {
        EpisodeLikeResponse episodeLikeResponse
                = episodeLikeService.like(principal.id(), episodeId);

        return ResponseEntity.ok(episodeLikeResponse);
    }

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
