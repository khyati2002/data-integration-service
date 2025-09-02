//package com.salescode.dis.insights.service;
//
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.io.IOException;
//import java.util.Set;
//import java.util.concurrent.*;
//
//@Service
//public class SseService {
//
//    private final ConcurrentMap<String, CopyOnWriteArraySet<SseEmitter>> statsEmitters = new ConcurrentHashMap<>();
//    private final ExecutorService sendExecutor = Executors.newCachedThreadPool();
//
//
//    public SseEmitter registerStatsEmitter(String key, long timeoutMillis) {
//        SseEmitter emitter = new SseEmitter(timeoutMillis);
//        statsEmitters.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>()).add(emitter);
//
//        emitter.onCompletion(() -> removeStatsEmitter(key, emitter));
//        emitter.onTimeout(() -> removeStatsEmitter(key, emitter));
//        emitter.onError((ex) -> removeStatsEmitter(key, emitter));
//
//        return emitter;
//    }
//
//    public void removeStatsEmitter(String key, SseEmitter emitter) {
//        Set<SseEmitter> set = statsEmitters.get(key);
//        if (set != null) {
//            set.remove(emitter);
//            if (set.isEmpty()) {
//                statsEmitters.remove(key);
//            }
//        }
//    }
//
//    public void statsBroadcastUpdate(String key, Object payload, String eventName) {
//        Set<SseEmitter> set = statsEmitters.get(key);
//        if (set == null || set.isEmpty()) return;
//
//        for (SseEmitter emitter : set) {
//            sendExecutor.submit(() -> {
//                try {
//                    SseEmitter.SseEventBuilder event = SseEmitter.event()
//                            .name(eventName)
//                            .data(payload, MediaType.APPLICATION_JSON);
//                    emitter.send(event);
//                } catch (IOException e) {
//                    removeStatsEmitter(key, emitter);
//                }
//            });
//        }
//    }
//    public Set<String> getActiveStatsKeys() {
//        return statsEmitters.keySet();
//    }
//
//    public void broadcastToAll(Object payload, String eventName) {
//        for (String key : statsEmitters.keySet()) {
//            statsBroadcastUpdate(key, payload, eventName);
//        }
//    }
//}
