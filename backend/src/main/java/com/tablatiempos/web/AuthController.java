package com.tablatiempos.web;

import com.tablatiempos.dto.AppInfo;
import com.tablatiempos.dto.LoginRequest;
import com.tablatiempos.service.AppSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppSettingsService settings;

    public AuthController(AppSettingsService settings) {
        this.settings = settings;
    }

    @PostMapping("/login")
    public AppInfo login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        if (!settings.isSetupComplete()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La aplicación no está configurada");
        }
        if (request == null || !settings.checkPassword(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contraseña incorrecta");
        }
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("auth", Boolean.TRUE);
        return new AppInfo(settings.appName(), true);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @GetMapping("/me")
    public AppInfo me() {
        return new AppInfo(settings.appName(), true);
    }
}
