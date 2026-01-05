package com.creatorhub.controller;


import com.creatorhub.dto.CreationRequest;
import com.creatorhub.service.CreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/creations")
@RequiredArgsConstructor
@Slf4j
public class CreationController {
    private final CreationService creationService;

    /**
     * 작품등록
     */
    @PostMapping("create")
    public ResponseEntity<Map<String, Object>> createCreation(@Valid @RequestBody CreationRequest req) {
        log.info("작품등록 요청 - creatorId={}, title={}, isPublic={}",
                req.creatorId(),
                req.title(),
                req.isPublic()
        );
        Long id = creationService.createCreation(req);
        return ResponseEntity.ok(Map.of("creationId", id));
    }

}
