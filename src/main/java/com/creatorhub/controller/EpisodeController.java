package com.creatorhub.controller;

import com.creatorhub.dto.episode.EpisodeRequest;
import com.creatorhub.dto.episode.EpisodeResponse;
import com.creatorhub.service.EpisodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/episodes")
@RequiredArgsConstructor
@Slf4j
public class EpisodeController {

    private final EpisodeService episodeService;

    /**
     * 회차 등록 + 원고/썸네일 매핑 insert
     */
    @PostMapping("/create")
    public ResponseEntity<EpisodeResponse> publishEpisode(@Valid @RequestBody EpisodeRequest req) {

        log.info("회차 등록 요청 - creationId={}, episodeNum={}, manuscripts={}, episodeFileObjectId={}, snsFileObjectId={}, isPublic={}, isCommentEnabled={}",
                req.creationId(),
                req.episodeNum(),
                req.manuscripts().size(),
                req.episodeFileObjectId(),
                req.snsFileObjectId(),
                req.isPublic(),
                req.isCommentEnabled()
        );

        EpisodeResponse res = episodeService.publishEpisode(req);
        return ResponseEntity.ok(res);
    }
}
