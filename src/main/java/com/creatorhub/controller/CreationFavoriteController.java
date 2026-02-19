package com.creatorhub.controller;


import com.creatorhub.dto.creation.CreationFavoriteResponse;
import com.creatorhub.dto.creation.favorite.FavoriteCreationItem;
import com.creatorhub.security.auth.CustomUserPrincipal;
import com.creatorhub.service.CreationFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @PostMapping("/{creationId}/favorite")
    public ResponseEntity<CreationFavoriteResponse> favorite(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long creationId
    ) {
        CreationFavoriteResponse creationFavoriteResponse =
                creationFavoriteService.favorite(principal.id(), creationId);

        return ResponseEntity.ok(creationFavoriteResponse);
    }

    /**
     * 관심 해제
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @DeleteMapping("/{creationId}/favorite")
    public ResponseEntity<CreationFavoriteResponse> unfavorite(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long creationId
    ) {
        CreationFavoriteResponse creationFavoriteResponse =
                creationFavoriteService.unfavorite(principal.id(), creationId);

        return ResponseEntity.ok(creationFavoriteResponse);
    }

    /**
     * 내 관심작품 목록
     */
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @GetMapping("/me/favorites")
    public ResponseEntity<Page<FavoriteCreationItem>> myFavorites(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            Pageable pageable
    ) {
        Page<FavoriteCreationItem> favoriteCreationItem =
                creationFavoriteService.getMyFavorites(principal.id(), pageable);

        return ResponseEntity.ok(favoriteCreationItem);
    }
}
