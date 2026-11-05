package com.tablatiempos.dto;

public record StageCellView(
        Long entrantId,
        Long millis,
        String kind,
        boolean best,
        boolean record,
        Long bestGapMs,
        Long prevGapMs,
        Integer prevPosition,
        String prevName,
        boolean provisional
) {
}
