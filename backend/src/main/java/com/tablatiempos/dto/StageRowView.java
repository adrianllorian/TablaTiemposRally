package com.tablatiempos.dto;

public record StageRowView(
        int position,
        Long entrantId,
        String name,
        String car,
        Long millis,
        String kind,
        boolean best,
        boolean record,
        Long bestGapMs,
        Long prevGapMs,
        Integer prevPosition,
        String prevName
) {
}
