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

    // 같은 baseKey 작업을 기다리는 SSE 연결이 웹 특성상 여러 개 동시에 생길 수 있으므로
    // 완료 이벤트를 모든 연결에 보내기 위해 List를 사용
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // baseKey: 원본 storeKey의 SUFFIX를 제외한 original 값
    // ex) upload/2026/01/14/5801402b-13b6-424e-bbb6-6ba9dfce4942
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