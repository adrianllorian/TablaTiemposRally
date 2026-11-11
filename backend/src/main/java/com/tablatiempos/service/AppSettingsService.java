package com.tablatiempos.service;

import com.tablatiempos.domain.AppSettings;
import com.tablatiempos.dto.SetupStatus;
import com.tablatiempos.repo.AppSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;

@Service
public class AppSettingsService {

    private static final long MAX_LOGO_BYTES = 2 * 1024 * 1024;
    private static final String DEFAULT_APP_NAME = "Tabla Tiempos Rally";

    private final AppSettingsRepository repository;
    private final Path logoDir;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AppSettingsService(AppSettingsRepository repository,
                              @Value("${app.data.dir}") String dataDir) {
        this.repository = repository;
        this.logoDir = Path.of(dataDir, "logo");
    }

    @Transactional(readOnly = true)
    public SetupStatus status() {
        AppSettings settings = settings();
        boolean complete = settings != null && settings.setupComplete;
        return new SetupStatus(
                complete,
                settings == null || settings.appName == null ? DEFAULT_APP_NAME : settings.appName,
                complete ? logoUrl(settings) : null
        );
    }

    @Transactional
    public SetupStatus setup(String password, String confirm, MultipartFile logo) {
        AppSettings settings = settings();
        if (settings != null && settings.setupComplete) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La aplicación ya está configurada");
        }
        validatePassword(password, confirm);

        if (settings == null) {
            settings = new AppSettings();
            settings.id = 1L;
        }
        settings.passwordHash = encoder.encode(password);
        settings.appName = DEFAULT_APP_NAME;
        settings.setupComplete = true;
        if (logo != null && !logo.isEmpty()) {
            storeLogo(settings, logo);
        }
        settings.updatedAt = Instant.now();
        repository.save(settings);
        return status();
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        AppSettings settings = requireSettings();
        if (currentPassword == null || !encoder.matches(currentPassword, settings.passwordHash)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
        }
        validatePassword(newPassword, confirmPassword);
        settings.passwordHash = encoder.encode(newPassword);
        settings.updatedAt = Instant.now();
        repository.save(settings);
    }

    @Transactional
    public SetupStatus updateLogo(MultipartFile logo) {
        AppSettings settings = requireSettings();
        if (logo == null || logo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una imagen");
        }
        storeLogo(settings, logo);
        settings.updatedAt = Instant.now();
        repository.save(settings);
        return status();
    }

    public boolean checkPassword(String rawPassword) {
        AppSettings settings = settings();
        return settings != null
                && settings.setupComplete
                && rawPassword != null
                && encoder.matches(rawPassword, settings.passwordHash);
    }

    public boolean isSetupComplete() {
        AppSettings settings = settings();
        return settings != null && settings.setupComplete;
    }

    public String appName() {
        AppSettings settings = settings();
        return settings == null || settings.appName == null ? DEFAULT_APP_NAME : settings.appName;
    }

    public LogoFile logoFile() {
        AppSettings settings = settings();
        if (settings == null || settings.logoFileName == null || settings.logoFileName.isBlank()) {
            return null;
        }
        Path path = logoDir.resolve(settings.logoFileName);
        if (!Files.exists(path)) {
            return null;
        }
        return new LogoFile(path, settings.logoContentType);
    }

    private AppSettings settings() {
        return repository.findById(1L).orElse(null);
    }

    private AppSettings requireSettings() {
        AppSettings settings = settings();
        if (settings == null || !settings.setupComplete) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La aplicación no está configurada");
        }
        return settings;
    }

    private void validatePassword(String password, String confirm) {
        if (password == null || password.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 4 caracteres");
        }
        if (!password.equals(confirm)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden");
        }
    }

    private void storeLogo(AppSettings settings, MultipartFile logo) {
        if (logo.getSize() > MAX_LOGO_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen no puede superar 2 MB");
        }
        String contentType = logo.getContentType() == null
                ? "application/octet-stream"
                : logo.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El logo debe ser una imagen");
        }
        String extension = switch (contentType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "image/svg+xml" -> "svg";
            default -> "img";
        };
        try {
            Files.createDirectories(logoDir);
            if (settings.logoFileName != null && !settings.logoFileName.isBlank()) {
                Files.deleteIfExists(logoDir.resolve(settings.logoFileName));
            }
            String fileName = "logo-" + System.currentTimeMillis() + "." + extension;
            Files.copy(logo.getInputStream(), logoDir.resolve(fileName));
            settings.logoFileName = fileName;
            settings.logoContentType = contentType;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el logo");
        }
    }

    private String logoUrl(AppSettings settings) {
        if (settings == null || settings.logoFileName == null || settings.logoFileName.isBlank()) {
            return null;
        }
        long version = settings.updatedAt == null ? 0 : settings.updatedAt.toEpochMilli();
        return "/api/public/logo?v=" + version;
    }

    public record LogoFile(Path path, String contentType) {
    }
}
