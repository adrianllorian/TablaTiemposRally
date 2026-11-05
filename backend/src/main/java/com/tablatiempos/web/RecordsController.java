package com.tablatiempos.web;

import com.tablatiempos.dto.RecordView;
import com.tablatiempos.service.RecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/records")
public class RecordsController {

    private final RecordService recordService;

    public RecordsController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public List<RecordView> all() {
        return recordService.all();
    }
}
