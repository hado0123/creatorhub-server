package com.creatorhub.controller;

import com.creatorhub.dto.hashtag.HashtagResponse;
import com.creatorhub.service.HashtagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hashtags")
@RequiredArgsConstructor
public class HashtagController {

    private final HashtagService hashtagService;

    /**
     * 전체 해시태그 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<HashtagResponse>> getAllHashtags() {
        return ResponseEntity.ok(hashtagService.getAllHashtags());
    }
}
