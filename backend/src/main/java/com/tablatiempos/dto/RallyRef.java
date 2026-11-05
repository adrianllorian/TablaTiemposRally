package com.tablatiempos.dto;

import java.time.LocalDate;

/**
 * Lightweight rally reference used when building historic records.
 * Keeps the query from returning a lazy Rally proxy, whose public fields are not
 * populated when read directly.
 */
public record RallyRef(
        Long id,
        String name,
        LocalDate date
) {
}
