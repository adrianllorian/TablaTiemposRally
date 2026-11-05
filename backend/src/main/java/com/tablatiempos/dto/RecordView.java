package com.tablatiempos.dto;

import java.time.LocalDate;
import java.util.List;

public record RecordView(
        String name,
        String key,
        String type,
        Long recordMs,
        String holder,
        String car,
        String rally,
        LocalDate date,
        List<RecordEntryView> top
) {
}
