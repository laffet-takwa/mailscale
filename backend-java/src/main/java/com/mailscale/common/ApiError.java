package com.mailscale.common;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId
) {
    public static ApiError of(int status, String error, String message,
                                String path, String correlationId) {
        return new ApiError(Instant.now(), status, error, message, path, correlationId);
    }
}
