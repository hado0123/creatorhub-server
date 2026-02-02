package com.creatorhub.common.sse;
import com.creatorhub.constant.SseEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class SseEmitters {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter add(String baseKey) {
        SseEmitter emitter = new SseEmitter(10L * 60 * 1000); // 10분
        emitters.computeIfAbsent(baseKey, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(baseKey, emitter));
        emitter.onTimeout(() -> remove(baseKey, emitter));
        emitter.onError(e -> remove(baseKey, emitter));

        return emitter;
    }

    public void send(String baseKey, SseEventType sseEventType, Object data) {
        List<SseEmitter> list = emitters.get(baseKey);
        if (list == null || list.isEmpty()) return;

        log.info("SSE send - baseKey={}, event={}, subscribers={}",
                baseKey, sseEventType, list.size());

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(String.valueOf(sseEventType)).data(data));
            } catch (IOException ex) {
                remove(baseKey, emitter);
            }
        }
    }

    private void remove(String baseKey, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(baseKey);
        if (list == null) return;

        list.remove(emitter);
        if (list.isEmpty()) emitters.remove(baseKey);
    }
}