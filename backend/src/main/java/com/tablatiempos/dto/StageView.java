package com.tablatiempos.dto;

import java.util.List;

public record StageView(
        Long id,
        String code,
        String name,
        String type,
        int position,
        String status,
        Long bestMs,
        Long bestEntrantId,
        Long recordMs,
        String recordHolder,
        String recordCar,
        String recordRally,
        String recordDate,
        List<StageCellView> cells,
        List<StageRowView> rows,
        List<String> missingNames
) {
}
