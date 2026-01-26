package com.creatorhub.controller;

import com.creatorhub.dto.episode.EpisodeRequest;
import com.creatorhub.dto.episode.EpisodeResponse;
import com.creatorhub.service.EpisodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<EpisodeResponse> publishEpisode(@Valid @RequestBody EpisodeRequest req) {
        EpisodeResponse res = episodeService.publishEpisode(req);
        return ResponseEntity.ok(res);
    }
}
