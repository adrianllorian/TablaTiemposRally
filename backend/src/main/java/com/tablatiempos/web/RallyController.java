package com.tablatiempos.web;

import com.tablatiempos.dto.RallyConfigRequest;
import com.tablatiempos.dto.RallySummary;
import com.tablatiempos.dto.RallyView;
import com.tablatiempos.dto.ResultRequest;
import com.tablatiempos.service.RallyService;
import com.tablatiempos.service.RallyViewService;
import com.tablatiempos.sse.RallyEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/rallies")
public class RallyController {

    private final RallyService rallyService;
    private final RallyViewService viewService;
    private final RallyEventService eventService;

    public RallyController(RallyService rallyService,
                           RallyViewService viewService,
                           RallyEventService eventService) {
        this.rallyService = rallyService;
        this.viewService = viewService;
        this.eventService = eventService;
    }

    @GetMapping
    public List<RallySummary> list() {
        return rallyService.list();
    }

    @PostMapping
    public RallyView create(@RequestBody RallyConfigRequest request) {
        RallyView view = rallyService.create(request);
        eventService.broadcast(view.id(), view);
        return view;
    }

    @GetMapping("/{id}")
    public RallyView get(@PathVariable Long id) {
        return viewService.view(id);
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable Long id) {
        RallyView view = viewService.view(id);
        SseEmitter emitter = eventService.subscribe(id);
        eventService.send(emitter, view);
        return emitter;
    }

    @PutMapping("/{id}/config")
    public RallyView updateConfig(@PathVariable Long id, @RequestBody RallyConfigRequest request) {
        RallyView view = rallyService.updateConfig(id, request);
        eventService.broadcast(id, view);
        return view;
    }

    @PutMapping("/{id}/results/{stageId}/{entrantId}")
    public RallyView setResult(@PathVariable Long id,
                               @PathVariable Long stageId,
                               @PathVariable Long entrantId,
                               @RequestBody ResultRequest request) {
        RallyView view = rallyService.setResult(id, stageId, entrantId, request);
        eventService.broadcast(id, view);
        return view;
    }

    @PostMapping("/{id}/entrants/{entrantId}/retire")
    public RallyView retire(@PathVariable Long id,
                            @PathVariable Long entrantId,
                            @RequestParam Long stageId) {
        RallyView view = rallyService.retire(id, entrantId, stageId);
        eventService.broadcast(id, view);
        return view;
    }

    @PostMapping("/{id}/reopen")
    public RallyView reopen(@PathVariable Long id) {
        RallyView view = rallyService.reopen(id);
        eventService.broadcast(id, view);
        return view;
    }

    @DeleteMapping("/{id}/entrants/{entrantId}/retire")
    public RallyView unretire(@PathVariable Long id, @PathVariable Long entrantId) {
        RallyView view = rallyService.unretire(id, entrantId);
        eventService.broadcast(id, view);
        return view;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        rallyService.delete(id);
    }
}
