package com.creatorhub.controller;


import com.creatorhub.dto.creation.CreationRequest;
import com.creatorhub.service.CreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Map<String, Object>> createCreation(@Valid @RequestBody CreationRequest req) {
        Long id = creationService.createCreation(req);
        return ResponseEntity.ok(Map.of("creationId", id));
    }

}
