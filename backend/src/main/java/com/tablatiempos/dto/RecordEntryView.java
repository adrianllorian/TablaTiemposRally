package com.tablatiempos.dto;

import java.time.LocalDate;

public record RecordEntryView(
        int position,
        String pilot,
        String car,
        Long millis,
        String rally,
        LocalDate date
) {
}
