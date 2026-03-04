package com.creatorhub.controller;


import com.creatorhub.constant.CreationSort;
import com.creatorhub.constant.PublishDay;
import com.creatorhub.dto.creation.*;
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
     * 요일별 연재 작품 조회
     */
    @GetMapping("/by-days")
    public CursorSliceResponse<CreationListItem> getCreationsByDay(
            @RequestParam PublishDay day,
            @RequestParam CreationSort sort,
            @RequestParam(defaultValue = "21") int size,

            // VIEWS / POPULAR 커서
            // 정렬 기준 값(조회순이면 작품 총 조회수, 인기순이면 작품 총 좋아요 수)
            @RequestParam(required = false) Long cursorValue,
            // 동점일 때 순서를 확정하는 기준 값(id로 한번더 정렬 - id DESC)
            @RequestParam(required = false) Long cursorId,

            // RATING 커서
            // 별점순에서 사용하는 평균 별점 값(이전 페이지 마지막 작품의 ratingAverage)
            @RequestParam(required = false) Double cursorAvg,
            // 별점 동점일 때 사용하는 평가 수(별점 평균이 같으면 평가 수가 많은 작품을 위로 정렬)
            @RequestParam(required = false) Long cursorRatingCount
    ) {
        SeekCursor cursor = switch (sort) {
            case VIEWS, POPULAR -> (cursorValue == null || cursorId == null)
                    ? null
                    : new SeekCursor(cursorId, cursorValue.doubleValue(), null);
            case RATING -> (cursorAvg == null || cursorRatingCount == null || cursorId == null)
                    ? null
                    : new SeekCursor(cursorId, cursorAvg, cursorRatingCount);
        };

        return creationService.getCreationsByDay(day, sort, cursor, size);
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
