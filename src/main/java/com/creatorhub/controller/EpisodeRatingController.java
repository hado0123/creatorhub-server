package com.creatorhub.controller;


import com.creatorhub.dto.episode.rating.EpisodeRatingRequest;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.EpisodeRatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/episodes/ratings")
public class EpisodeRatingController {

    private final EpisodeRatingService episodeRatingService;

    /**
     * 별점 등록
     */
    @PostMapping("/create")
    public ResponseEntity<Void> rate(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody EpisodeRatingRequest req
    ) {
        episodeRatingService.rate(principal.id(), req.episodeId(), req.score());
        return ResponseEntity.ok().build();
    }
}