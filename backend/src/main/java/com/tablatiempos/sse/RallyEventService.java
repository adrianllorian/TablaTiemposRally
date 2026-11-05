package com.tablatiempos.sse;

import com.tablatiempos.dto.RallyView;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RallyEventService {

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

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
            emitter.send(SseEmitter.event()
                    .name("rally")
                    .data(view, MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
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
                emitter.send(SseEmitter.event()
                        .name("rally")
                        .data(view, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                remove(rallyId, emitter);
                emitter.complete();
            }
        }
    }

    private void remove(Long rallyId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(rallyId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
