package com.tablatiempos.dto;

import java.util.List;

public record DictionariesView(
        List<String> stages,
        List<String> cars,
        List<String> pilots
) {
}
