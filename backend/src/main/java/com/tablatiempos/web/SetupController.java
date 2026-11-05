package com.tablatiempos.web;

import com.tablatiempos.dto.SetupStatus;
import com.tablatiempos.service.AppSettingsService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/public")
public class SetupController {

    private final AppSettingsService settings;

    public SetupController(AppSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping("/status")
    public SetupStatus status() {
        return settings.status();
    }

    @PostMapping("/setup")
    public SetupStatus setup(@RequestParam String password,
                             @RequestParam String confirm,
                             @RequestParam(value = "logo", required = false) MultipartFile logo) {
        return settings.setup(password, confirm, logo);
    }

    @GetMapping("/logo")
    public ResponseEntity<Resource> logo() {
        AppSettingsService.LogoFile file = settings.logoFile();
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.noCache())
                .body(new FileSystemResource(file.path()));
    }
}
