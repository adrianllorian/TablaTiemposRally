package com.tablatiempos.web;

import com.tablatiempos.dto.RallyView;
import com.tablatiempos.service.ExportService;
import com.tablatiempos.service.RallyViewService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/rallies")
public class ExportController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportService exportService;
    private final RallyViewService viewService;

    public ExportController(ExportService exportService, RallyViewService viewService) {
        this.exportService = exportService;
        this.viewService = viewService;
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id) throws IOException {
        RallyView view = viewService.view(id);
        byte[] file = exportService.export(id);
        String baseName = slug(view.name()) + "-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(baseName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(file);
    }

    private String slug(String value) {
        String cleaned = value == null ? "rally" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return cleaned.isBlank() ? "rally" : cleaned;
    }
}
