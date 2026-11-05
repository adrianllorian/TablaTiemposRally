package com.tablatiempos.dto;

import java.util.List;

public record RallyConfigRequest(
        String name,
        List<StageInput> stages,
        List<EntrantInput> entrants,
        Boolean shakedownEnabled,
        String shakedownName,
        String penaltyMode,
        Long penaltyValueMs
) {
    public record StageInput(String name) {
    }

    public record EntrantInput(String name, String car) {
    }
}
