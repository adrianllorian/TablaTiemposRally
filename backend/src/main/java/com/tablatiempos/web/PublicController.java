package com.tablatiempos.web;

import com.tablatiempos.dto.AppInfo;
import com.tablatiempos.dto.RallyView;
import com.tablatiempos.service.AppSettingsService;
import com.tablatiempos.service.RallyViewService;
import com.tablatiempos.sse.RallyEventService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final AppSettingsService settings;
    private final RallyViewService viewService;
    private final RallyEventService eventService;

    public PublicController(AppSettingsService settings,
                            RallyViewService viewService,
                            RallyEventService eventService) {
        this.settings = settings;
        this.viewService = viewService;
        this.eventService = eventService;
    }

    @GetMapping("/app")
    public AppInfo app() {
        return new AppInfo(settings.appName(), false);
    }

    @GetMapping("/rallies/{token}")
    public RallyView get(@PathVariable String token) {
        return viewService.viewByToken(token);
    }

    @GetMapping(value = "/rallies/{token}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String token) {
        RallyView view = viewService.viewByToken(token);
        SseEmitter emitter = eventService.subscribe(view.id());
        eventService.send(emitter, view);
        return emitter;
    }
}
