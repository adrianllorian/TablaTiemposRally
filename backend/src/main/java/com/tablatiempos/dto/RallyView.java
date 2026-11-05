package com.tablatiempos.dto;

import java.time.LocalDate;
import java.util.List;

public record RallyView(
        Long id,
        String name,
        LocalDate date,
        String status,
        boolean shakedownEnabled,
        String shakedownName,
        String penaltyMode,
        long penaltyValueMs,
        String inviteToken,
        long version,
        List<StageView> stages,
        List<GeneralRowView> general,
        List<EntrantView> entrants,
        Long currentStageId,
        String currentStageCode,
        String currentStageStatus,
        List<String> missingNames
) {
}
