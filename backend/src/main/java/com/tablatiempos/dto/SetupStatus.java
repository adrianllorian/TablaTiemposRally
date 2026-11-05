package com.tablatiempos.dto;

public record SetupStatus(
        boolean setupComplete,
        String appName,
        String logoUrl
) {
}
