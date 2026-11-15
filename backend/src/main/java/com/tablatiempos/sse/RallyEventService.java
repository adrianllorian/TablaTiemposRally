package com.tablatiempos.sse;

import com.tablatiempos.dto.RallyView;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class RallyEventService {

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    @PostConstruct
    void startHeartbeat() {
        heartbeat.scheduleAtFixedRate(this::pingAll, 15, 15, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stopHeartbeat() {
        heartbeat.shutdownNow();
    }

    public SseEmitter subscribe(Long rallyId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(rallyId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(rallyId, emitter));
        emitter.onTimeout(() -> {
            remove(rallyId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> remove(rallyId, emitter));
        return emitter;
    }

    public void send(SseEmitter emitter, RallyView view) {
        try {
            sendEvent(emitter, SseEmitter.event()
                    .name("rally")
                    .data(view, MediaType.APPLICATION_JSON));
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }

    public void broadcast(Long rallyId, RallyView view) {
        List<SseEmitter> list = emitters.get(rallyId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                sendEvent(emitter, SseEmitter.event()
                        .name("rally")
                        .data(view, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                discard(rallyId, emitter);
            }
        }
    }

    private void pingAll() {
        emitters.forEach((rallyId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    sendEvent(emitter, SseEmitter.event().comment("ping"));
                } catch (Exception ex) {
                    discard(rallyId, emitter);
                }
            }
        });
    }

    private void sendEvent(SseEmitter emitter, SseEmitter.SseEventBuilder event) throws IOException {
        // Varias peticiones pueden terminar a la vez; el emisor no es thread-safe.
        synchronized (emitter) {
            emitter.send(event);
        }
    }

    private void discard(Long rallyId, SseEmitter emitter) {
        remove(rallyId, emitter);
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // El cliente ya se había desconectado.
        }
    }

    private void remove(Long rallyId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(rallyId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
