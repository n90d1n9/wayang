/*
 * PolyForm Noncommercial License 1.0.0
 *
 * Copyright (c) 2026 Kayys.tech
 *
 * This software is licensed for non-commercial use only.
 * You may use, modify, and distribute this software for personal,
 * educational, or research purposes.
 *
 * Commercial use, including SaaS or revenue-generating services,
 * requires a separate commercial license from Kayys.tech.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 *
 * @author Bhangun
 */

package tech.kayys.wayang.error;

import java.time.Instant;

/**
 * Standard error response for Wayang services.
 */
public record ErrorResponse(
        String error,
        String message,
        String code,
        int httpStatus,
        String requestId,
        Instant timestamp,
        boolean retryable) {

    public static ErrorResponse from(Throwable th) {
        if (th instanceof WayangException we) {
            ErrorCode errorCode = we.getErrorCode();
            return new ErrorResponse(
                    we.getClass().getSimpleName(),
                    we.getMessage(),
                    errorCode.getCode(),
                    errorCode.getHttpStatus(),
                    we.getRequestId(),
                    we.getTimestamp(),
                    errorCode.isRetryable());
        }

        ErrorCode fallback = ErrorCode.INTERNAL_ERROR;
        return new ErrorResponse(
                th.getClass().getSimpleName(),
                th.getMessage(),
                fallback.getCode(),
                fallback.getHttpStatus(),
                null, // No request ID
                Instant.now(),
                fallback.isRetryable());
    }
}
