package com.tablatiempos.web;

import com.tablatiempos.dto.ChangePasswordRequest;
import com.tablatiempos.dto.SetupStatus;
import com.tablatiempos.service.AppSettingsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final AppSettingsService settings;

    public SettingsController(AppSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public SetupStatus status() {
        return settings.status();
    }

    @PutMapping("/password")
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        settings.changePassword(
                request.currentPassword(),
                request.newPassword(),
                request.confirmPassword()
        );
    }

    @PostMapping("/logo")
    public SetupStatus updateLogo(@RequestParam("logo") MultipartFile logo) {
        return settings.updateLogo(logo);
    }
}
