package com.creatorhub.controller;

import com.creatorhub.dto.episode.like.EpisodeLikeRequest;
import com.creatorhub.dto.episode.like.EpisodeLikeResponse;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.EpisodeLikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/episodes/likes")
public class EpisodeLikeController {
    private final EpisodeLikeService episodeLikeService;

    @PostMapping("/create")
    public ResponseEntity<EpisodeLikeResponse> like(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody EpisodeLikeRequest req
    ) {
        EpisodeLikeResponse episodeLikeResponse
                = episodeLikeService.like(principal.id(), req.episodeId());

        return ResponseEntity.ok(episodeLikeResponse);
    }

    @DeleteMapping("/delete/{episodeId}")
    public ResponseEntity<EpisodeLikeResponse> unlike(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long episodeId
    ) {
        EpisodeLikeResponse episodeLikeResponse
                = episodeLikeService.unlike(principal.id(), episodeId);

        return ResponseEntity.ok(episodeLikeResponse);
    }
}
