package com.tablatiempos.dto;

import java.time.LocalDate;

public record RallySummary(
        Long id,
        String name,
        LocalDate date,
        String status,
        String leader,
        String winner,
        int stagesTotal,
        int stagesClosed,
        boolean active,
        String inviteToken
) {
}
