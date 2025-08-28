package com.salescode.dis.insights.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class SseService {

    private final ConcurrentMap<String, CopyOnWriteArraySet<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ExecutorService sendExecutor = Executors.newCachedThreadPool();


    public SseEmitter register(String key, long timeoutMillis) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        emitters.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError((ex) -> removeEmitter(key, emitter));

        return emitter;
    }

    public void removeEmitter(String key, SseEmitter emitter) {
        Set<SseEmitter> set = emitters.get(key);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) {
                emitters.remove(key);
            }
        }
    }

    public void broadcast(String key, Object payload, String eventName) {
        Set<SseEmitter> set = emitters.get(key);
        if (set == null || set.isEmpty()) return;

        for (SseEmitter emitter : set) {
            sendExecutor.submit(() -> {
                try {
                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                            .name(eventName)
                            .data(payload, MediaType.APPLICATION_JSON);
                    emitter.send(event);
                } catch (IOException e) {
                    removeEmitter(key, emitter);
                }
            });
        }
    }
    public Set<String> getActiveKeys() {
        return emitters.keySet();
    }

    public void broadcastToAll(Object payload, String eventName) {
        for (String key : emitters.keySet()) {
            broadcast(key, payload, eventName);
        }
    }
}
