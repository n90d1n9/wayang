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
package tech.kayys.wayang.plugin.execution;

import java.util.HashMap;
import java.util.Map;

import tech.kayys.wayang.error.ErrorCode;

/**
 * Execution Error - Structured error information
 */
public class ExecutionError {
    public String code;
    public String message;
    public String type;
    public Map<String, Object> details = new HashMap<>();
    public String stackTrace;
    public boolean retryable;

    public static ExecutionError from(ErrorCode errorCode, String message) {
        ExecutionError error = new ExecutionError();
        error.code = errorCode.getCode();
        error.message = message != null ? message : errorCode.getDefaultMessage();
        error.type = errorCode.getCategory().getPrefix();
        error.retryable = errorCode.isRetryable();
        return error;
    }
}
