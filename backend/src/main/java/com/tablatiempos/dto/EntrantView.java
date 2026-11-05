package com.tablatiempos.dto;

public record EntrantView(
        Long id,
        String name,
        String car,
        int position,
        boolean retired,
        Integer retiredAtPosition
) {
}
