package com.creatorhub.controller;


import com.creatorhub.dto.CreationFavoriteRequest;
import com.creatorhub.dto.CreationFavoriteResponse;
import com.creatorhub.dto.FavoriteCreationItem;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.CreationFavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/creations")
public class CreationFavoriteController {
    private final CreationFavoriteService creationFavoriteService;

    /**
     * 관심 등록
     */
    @PostMapping("/favorites")
    public ResponseEntity<CreationFavoriteResponse> addFavorite(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreationFavoriteRequest req
    ) {
        CreationFavoriteResponse creationFavoriteResponse =
                creationFavoriteService.addFavorite(principal.id(), req.creationId());

        return ResponseEntity.ok(creationFavoriteResponse);
    }

    /**
     * 관심 해제
     */
    @DeleteMapping("/favorites/{creationId}")
    public ResponseEntity<CreationFavoriteResponse> removeFavorite(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long creationId
    ) {
        CreationFavoriteResponse creationFavoriteResponse =
                creationFavoriteService.removeFavorite(principal.id(), creationId);

        return ResponseEntity.ok(creationFavoriteResponse);
    }

    /**
     * 내 관심작품 목록
     */
    @GetMapping("/me")
    public ResponseEntity<Page<FavoriteCreationItem>> myFavorites(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            Pageable pageable
    ) {
        Page<FavoriteCreationItem> favoriteCreationItem =
                creationFavoriteService.getMyFavorites(principal.id(), pageable);

        return ResponseEntity.ok(favoriteCreationItem);
    }
}
