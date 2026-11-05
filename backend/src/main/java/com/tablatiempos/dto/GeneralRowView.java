package com.tablatiempos.dto;

public record GeneralRowView(
        Integer position,
        Long entrantId,
        String name,
        String car,
        String status,
        Long totalMs,
        Long partialMs,
        Long difLeaderMs,
        Long difPrevMs,
        Integer prevPosition,
        String prevName,
        boolean leader
) {
}
