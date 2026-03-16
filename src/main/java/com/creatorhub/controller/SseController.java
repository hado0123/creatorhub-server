package com.creatorhub.controller;

import com.creatorhub.common.sse.SseEmitters;
import com.creatorhub.constant.SseEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {
    private final SseEmitters sseEmitters;

    /**
     * SSE 최초 구독
     */
    @PreAuthorize("hasRole('ROLE_CREATOR')")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String baseKey) {
        SseEmitter emitter = sseEmitters.add(baseKey);
        sseEmitters.send(baseKey, SseEventType.CONNECTED, Map.of(
                "baseKey", baseKey,
                "serverTime", System.currentTimeMillis()
        ));
        return emitter;
    }
}
