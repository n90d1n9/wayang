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

import java.util.Objects;

/**
 * Base exception carrying a Wayang {@link ErrorCode}.
 */
public class WayangException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String requestId;
    private final java.time.Instant timestamp;

    public WayangException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public WayangException(ErrorCode errorCode, String message, String requestId) {
        this(errorCode, message, requestId, null);
    }

    public WayangException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause);
    }

    public WayangException(ErrorCode errorCode, String message, String requestId, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
        this.requestId = requestId;
        this.timestamp = java.time.Instant.now();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getRequestId() {
        return requestId;
    }

    public java.time.Instant getTimestamp() {
        return timestamp;
    }
}
