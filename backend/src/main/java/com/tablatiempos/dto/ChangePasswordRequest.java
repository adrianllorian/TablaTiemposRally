package com.tablatiempos.dto;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword,
        String confirmPassword
) {
}
